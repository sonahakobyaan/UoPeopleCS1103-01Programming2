/** A book in the library collection. */
public class Book extends LibraryItem<String> {

    private final int pages;

    /**
     * @param itemID unique ID of the book
     * @param title  book title
     * @param author book author
     * @param pages  number of pages (must be greater than 0)
     */
    public Book(String itemID, String title, String author, int pages) {
        super(itemID, title, author);
        if (pages <= 0) {
            throw new IllegalArgumentException("Pages must be greater than 0.");
        }
        this.pages = pages;
    }

    public int getPages() {
        return pages;
    }

    @Override
    public String getItemType() {
        return "Book";
    }

    @Override
    public String getDetails() {
        return super.getDetails() + " | Pages: " + pages;
    }
}
