package by.slizh.calculator

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import by.slizh.calculator.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var canAddOperation = false
    private var canAddDecimal = true
    private var lastResult: String = ""
    private var lastCalculation: String = ""
    private var isNewCalculationStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setListeners()
        setEquationTextWatcher()
    }

    private fun getNumberButton() = with(binding) {
        arrayOf(
            zeroButton,
            oneButton,
            twoButton,
            threeButton,
            fourButton,
            fiveButton,
            sixButton,
            sevenButton,
            eightButton,
            nineButton,
        )
    }

    private fun setListeners() {
        for (button in getNumberButton()) {
            button.setOnClickListener { onNumberClicked(button.text.toString()) }
        }

        with(binding) {
            decimalPointButton.setOnClickListener { onDecimalPointClicked() }
            additionButton.setOnClickListener { onOperatorClicked(it) }
            subtractionButton.setOnClickListener { onOperatorClicked(it) }
            multiplicationButton.setOnClickListener { onOperatorClicked(it) }
            divisionButton.setOnClickListener { onOperatorClicked(it) }
            equalsButton.setOnClickListener { onEqualsClicked() }
            allDeleteButton.setOnClickListener { onAllDeleteClicked() }
            backspaceButton.setOnClickListener { onBackspaceClicked() }
        }
    }

    private fun setEquationTextWatcher() {
        binding.equationTextView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                setTextSize()
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setTextSize() {
        val text = binding.equationTextView.text.toString()
        val textSize = when {
            text.length <= 10 -> 48f
            text.length <= 20 -> 38f
            text.length <= 40 -> 28f
            text.length <= 60 -> 18f
            else -> 15f
        }
        binding.equationTextView.textSize = textSize
    }

    private fun onNumberClicked(number: String) {
        if (lastCalculation.isNotEmpty()) {
            binding.equationTextView.text = ""
            lastCalculation = ""
        }
        binding.equationTextView.append(number)
        canAddOperation = true
    }

    private fun onDecimalPointClicked() {
        if (canAddDecimal) binding.equationTextView.append(DECIMAL_POINT.toString())
        canAddDecimal = false
    }

    private fun onOperatorClicked(view: View) {
        if (view is Button) {
            if (lastCalculation.isNotEmpty()) {
                if (isNewCalculationStarted) lastResult = ZERO
                binding.equationTextView.text = lastResult
                lastCalculation = ""
            }
            if (canAddOperation || view.text.toString() == "-") {
                binding.equationTextView.append(view.text)
                canAddOperation = false
                canAddDecimal = true
            }
            isNewCalculationStarted = false
        }
    }

    private fun onAllDeleteClicked() {
        binding.equationTextView.text = ""
        binding.resultTextView.text = ""
        lastResult = ""
        lastCalculation = ""
        isNewCalculationStarted = false
    }

    private fun onBackspaceClicked() {
        val length = binding.equationTextView.length()
        if (length > 0)
            binding.equationTextView.text = binding.equationTextView.text.subSequence(0, length - 1)
    }

    private fun onEqualsClicked() {
        val result = calculateResults()
        binding.resultTextView.text = result
        lastResult = result
        lastCalculation = binding.equationTextView.text.toString()
    }

    private fun calculateResults(): String {
        val equation = createEquation()
        val timesDivision = getMultiplicationDivisionResult(equation)

        if (equation.isEmpty() || timesDivision.isEmpty()) return ""

        val result = additionSubtractionCalculate(timesDivision)

        if (result.isInfinite() || result.isNaN()) isNewCalculationStarted = true

        return result.toString()
    }

    private fun additionSubtractionCalculate(currentEquation: MutableList<Any>): Double {
        var result = currentEquation[0] as Double

        for (i in currentEquation.indices) {
            if (currentEquation[i] is Char && i != currentEquation.lastIndex) {
                val operator = currentEquation[i]
                val nextDigit = currentEquation[i + 1] as Double
                when (operator) {
                    PLUS -> result += nextDigit
                    '−' -> result -= nextDigit
                }
            }
        }
        return result
    }

    private fun getMultiplicationDivisionResult(currentEquation: MutableList<Any>): MutableList<Any> {
        var equation = currentEquation
        while (equation.contains(MULTIPLICATION) || equation.contains(DIVISION)) {
            equation = multiplicationDivisionCalculate(equation)
        }
        return equation
    }

    private fun multiplicationDivisionCalculate(currentEquation: MutableList<Any>): MutableList<Any> {
        val newEquation = mutableListOf<Any>()
        var currentListSize = currentEquation.size

        for (i in currentEquation.indices) {
            if (currentEquation[i] is Char && i != currentEquation.lastIndex && i < currentListSize) {
                val operator = currentEquation[i]
                val previousDigit = currentEquation[i - 1] as Double
                val nextDigit = currentEquation[i + 1] as Double
                when (operator) {
                    MULTIPLICATION -> {
                        newEquation.add(previousDigit * nextDigit)
                        currentListSize = i + 1
                    }

                    DIVISION -> {
                        newEquation.add(previousDigit / nextDigit)
                        currentListSize = i + 1
                    }

                    else -> {
                        newEquation.add(previousDigit)
                        newEquation.add(operator)
                    }
                }
            }

            if (i > currentListSize)
                newEquation.add(currentEquation[i])
        }

        return newEquation
    }

    private fun createEquation(): MutableList<Any> {
        val equation = mutableListOf<Any>()
        var currentDigit = ""
        var lastOperator = ' '
        for (character in binding.equationTextView.text) {
            if (character.isDigit() || character == DECIMAL_POINT || (character == '-' && lastOperator !in listOf(
                    MULTIPLICATION,
                    DIVISION,
                    PLUS,
                    '-'
                ))
            ) {
                currentDigit += character
            } else {
                if (currentDigit.isNotEmpty()) {
                    equation.add(currentDigit.toDouble())
                }
                currentDigit = ""
                equation.add(character)
                lastOperator = character
            }
        }

        if (currentDigit.isNotEmpty()) {
            equation.add(currentDigit.toDouble())
        }

        return equation
    }

    companion object {
        const val ZERO = "0"
        const val DECIMAL_POINT = '.'
        const val MINUS = '−'
        const val PLUS = '+'
        const val DIVISION = '÷'
        const val MULTIPLICATION = '×'
    }
}