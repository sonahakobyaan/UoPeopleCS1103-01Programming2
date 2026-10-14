/** A magazine issue in the library collection. */
public class Magazine extends LibraryItem<String> {

    private final int issueNumber;

    /**
     * @param itemID      unique ID of the magazine
     * @param title       magazine title
     * @param author      publisher / editor
     * @param issueNumber issue number (must be greater than 0)
     */
    public Magazine(String itemID, String title, String author, int issueNumber) {
        super(itemID, title, author);
        if (issueNumber <= 0) {
            throw new IllegalArgumentException("Issue number must be greater than 0.");
        }
        this.issueNumber = issueNumber;
    }

    public int getIssueNumber() {
        return issueNumber;
    }

    @Override
    public String getItemType() {
        return "Magazine";
    }

    @Override
    public String getDetails() {
        return super.getDetails() + " | Issue: #" + issueNumber;
    }
}
