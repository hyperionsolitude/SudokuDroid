package com.sudokudroid.game

import kotlin.random.Random

enum class Difficulty(val cellsToRemove: Int) {
    EASY(30),
    MEDIUM(40),
    HARD(50)
}

class SudokuGameLogic {
    private val size = 9
    private val board = Array(size) { IntArray(size) }
    private val solution = Array(size) { IntArray(size) }

    fun generateGame(difficulty: Difficulty): Array<IntArray> {
        // 1. Clear board
        for (i in 0 until size) for (j in 0 until size) board[i][j] = 0

        // 2. Fill the board (solve it from scratch)
        fillBoard()

        // 3. Save the solution
        for (i in 0 until size) {
            for (j in 0 until size) {
                solution[i][j] = board[i][j]
            }
        }

        // 4. Remove numbers based on difficulty
        removeNumbers(difficulty.cellsToRemove)

        return board
    }

    private fun fillBoard(): Boolean {
        for (row in 0 until size) {
            for (col in 0 until size) {
                if (board[row][col] == 0) {
                    val numbers = (1..9).shuffled().toList()
                    for (num in numbers) {
                        if (isValid(board, row, col, num)) {
                            board[row][col] = num
                            if (fillBoard()) return true
                            board[row][col] = 0
                        }
                    }
                    return false
                }
            }
        }
        return true
    }

    private fun isValid(board: Array<IntArray>, row: Int, col: Int, num: Int): Boolean {
        for (i in 0 until size) {
            if (board[row][i] == num || board[i][col] == num) return false
        }

        val startRow = (row / 3) * 3
        val startCol = (col / 3) * 3
        for (i in 0 until 3) {
            for (j in 0 until 3) {
                if (board[startRow + i][startCol + j] == num) return false
            }
        }
        return true
    }

    private fun removeNumbers(count: Int) {
        var removed = 0
        while (removed < count) {
            val row = Random.nextInt(size)
            val col = Random.nextInt(size)
            if (board[row][col] != 0) {
                board[row][col] = 0
                removed++
            }
        }
    }

    fun checkCell(row: Int, col: Int, num: Int): Boolean {
        return solution[row][col] == num
    }

    fun getSolution(): Array<IntArray> = solution
}
