import com.example.noteon.Note
import kotlin.random.Random

object DataHandler {
    private val notes = mutableListOf<Note>()
    private var lastId = 0L

    private val dummyTitles = listOf(
        "Grocery List", "Meeting Notes", "Book Ideas", "Travel Plans",
        "Fitness Goals", "Recipe", "Movie Recommendations", "Birthday Gift Ideas",
        "Project Brainstorm", "Quotes", "Bucket List", "Homework Assignments"
    )

    private val dummyContents = listOf(
        "Remember to buy milk, eggs, and bread.",
        "Discuss Q3 goals and review last month's performance.",
        "A mystery novel set in a small town with a twist ending.",
        "Research hotels in Paris and book flights for next month.",
        "Run 5k three times a week and do strength training on weekends.",
        "Ingredients: flour, sugar, eggs. Instructions: Mix and bake at 350°F.",
        "1. The Shawshank Redemption\n2. Inception\n3. The Matrix",
        "Mom: scarf, Dad: book, Sister: headphones",
        "New app idea: AI-powered plant care assistant",
        "\"The only way to do great work is to love what you do.\" - Steve Jobs",
        "1. Skydiving\n2. Learn a new language\n3. Visit all 7 continents",
        "Math: pg 45-47, Science: lab report, English: essay outline"
    )

    fun generateDummyNotes(count: Int): List<Note> {
        notes.clear()
        repeat(count) {
            val title = dummyTitles[Random.nextInt(dummyTitles.size)]
            val content = dummyContents[Random.nextInt(dummyContents.size)]
            val timestamp = System.currentTimeMillis() - Random.nextLong(0, 30L * 24 * 60 * 60 * 1000) // Random time within last 30 days
            notes.add(Note(id = ++lastId, title = title, content = content, timestamp = timestamp))
        }
        return notes.toList()
    }

    fun getNoteById(id: Long): Note? {
        return notes.find { it.id == id }
    }

    fun getAllNotes(): List<Note> = notes.toList()
}