/** A DVD in the library collection. */
public class DVD extends LibraryItem<String> {

    private final int durationMinutes;

    /**
     * @param itemID          unique ID of the DVD
     * @param title           movie title
     * @param author          director / creator
     * @param durationMinutes running time in minutes (must be greater than 0)
     */
    public DVD(String itemID, String title, String author, int durationMinutes) {
        super(itemID, title, author);
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("Duration must be greater than 0.");
        }
        this.durationMinutes = durationMinutes;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    @Override
    public String getItemType() {
        return "DVD";
    }

    @Override
    public String getDetails() {
        return super.getDetails() + " | Duration: " + durationMinutes + " min";
    }
}
