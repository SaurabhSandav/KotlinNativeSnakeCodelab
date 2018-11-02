import kotlinx.cinterop.CPointer
import kotlinx.cinterop.memScoped
import ncurses.*
import kotlin.random.Random

fun main(args: Array<String>) = memScoped {
    initscr()
    defer { endwin() }
    noecho()
    curs_set(0)

    halfdelay(2)

    var game = Game(
            width = 20,
            height = 10,
            snake = Snake(
                    cells = listOf(Cell(4, 0), Cell(3, 0), Cell(2, 0), Cell(1, 0), Cell(0, 0)),
                    direction = Direction.RIGHT,
                    eatenApples = 0
            )
    )

    val window = newwin(game.height + 2, game.width + 2, 0, 0)!!
    defer { delwin(window) }

    var input = 0

    while (input.toChar() != 'q') {
        window.draw(game)

        input = wgetch(window)
        val direction = when (input.toChar()) {
            'i' -> Direction.UP
            'j' -> Direction.LEFT
            'k' -> Direction.DOWN
            'l' -> Direction.RIGHT
            else -> null
        }

        game = game.update(direction)
    }
}

fun CPointer<WINDOW>.draw(game: Game) {
    wclear(this)
    box(this, 0, 0)

    game.apples.cells.forEach { mvwprintw(this, it.y + 1, it.x + 1, ".") }
    game.snake.tail.forEach { mvwprintw(this, it.y + 1, it.x + 1, "o") }
    game.snake.head.let { mvwprintw(this, it.y + 1, it.x + 1, "Q") }

    if (game.isOver) {
        mvwprintw(this, 0, 6, "Game over!")
        mvwprintw(this, 1, 3, "Your score is ${game.score}")
    }

    wrefresh(this)
}

data class Game(
        val width: Int,
        val height: Int,
        val snake: Snake,
        val apples: Apples = Apples(width, height)
) {

    val score = snake.cells.size
    val isOver = snake.tail.contains(snake.head) || snake.cells.any {
        it.x < 0 || it.x >= width || it.y < 0 || it.y >= height
    }

    fun update(direction: Direction?): Game {
        if (isOver) return this

        val (newSnake, newApples) = snake.turn(direction)
                .move()
                .eat(apples.grow())

        return copy(
                snake = newSnake,
                apples = newApples
        )
    }
}

data class Apples(
        val width: Int,
        val height: Int,
        val cells: Set<Cell> = emptySet(),
        val growthSpeed: Int = 3,
        val random: Random = Random
) {

    fun grow(): Apples {
        if (random.nextInt(10) >= 3) return this
        return copy(
                cells = cells + Cell(Random.nextInt(width), Random.nextInt(height))
        )
    }
}

data class Snake(val cells: List<Cell>, val direction: Direction, val eatenApples: Int) {

    val head = cells.first()
    val tail = cells.subList(1, cells.size)

    fun move(): Snake {
        val newHead = cells.first().move(direction)
        val newTail = if (eatenApples == 0) cells.dropLast(1) else cells
        return copy(
                cells = listOf(newHead) + newTail,
                eatenApples = maxOf(0, eatenApples - 1)
        )
    }

    fun turn(newDirection: Direction?): Snake {
        if (newDirection == null || newDirection.isOpposite(direction)) return this
        return copy(direction = newDirection)
    }

    fun eat(apples: Apples): Pair<Snake, Apples> {
        if (!apples.cells.contains(head)) return Pair(this, apples)
        return Pair(
                this.copy(eatenApples = eatenApples + 1),
                apples.copy(cells = apples.cells - head)
        )
    }
}

data class Cell(val x: Int, val y: Int) {

    fun move(direction: Direction) = Cell(x + direction.dx, y + direction.dy)
}

enum class Direction(val dx: Int, val dy: Int) {
    UP(0, -1),
    DOWN(0, 1),
    LEFT(-1, 0),
    RIGHT(1, 0);

    fun isOpposite(that: Direction) = dx + that.dx == 0 && dy + that.dy == 0
}
