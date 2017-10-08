import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import org.osbot.rs07.api.model.Item;

public class ItemRequirement {

    private List<RequiredItem> items;

    public ItemRequirement(List<RequiredItem> items) {
        this.items = items;
    }

    /**
     * If an item to check for has a charge or dose is in its name, e.g. Ring of
     * dueling(4), it will also check for the item with additional
     * charges/doses, up to the value of RequiredItem's range field.
     * <p>
     * Example: item is Ring of dueling(4) with a range of 3
     * will match Ring of dueling(4), (5), (6), and (7)
     *
     * @param itemsToCheck the items to check against
     *
     * @return a map of the missing items in (item name : missing amount) pairs.
     *         If no items are missing the map will be empty.
     */
    public HashMap<String, Integer> getMissingItems(Item[] itemsToCheck) {
        HashMap<String, Integer> missingItems = new HashMap<>();
        List<Item> itemList = Arrays.asList(itemsToCheck); // list rep. of items to check against

        for (RequiredItem item : this.items) {
            int desiredAmount = item.getAmount();
            int charge = getItemCharge(item.getName());
            List<Item> filteredList;
            
            // filtered list for item if it's noted
            if (item.isNoted()) {
                filteredList = itemList.stream()
                        .filter(i -> (i != null && i.getName().toLowerCase().equals(item.getName().toLowerCase()) && i.isNote()))
                        .collect(Collectors.toList());
            }
            else { // item isn't noted
                filteredList = itemList.stream()
                        .filter(i -> (i != null && i.getName().toLowerCase().equals(item.getName().toLowerCase())))
                        .collect(Collectors.toList());
            }

            // filtered list if the item isn't noted
            if (filteredList.size() > 0) {
                // if the item we're checking for contains a charge/dose, check to
                // see if we have an item with that charge/dose, up to (starting charge/dose + range)
                if (charge > 0) {
                    int itemCount = 0; // keep track of how many items we have

                    StringBuilder itemName = new StringBuilder(item.getName());
                    int openIndex = itemName.indexOf("(");

                    int x = 0;
                    do {
                        // if we have the item and it's noted, increase by the quantity of the note
                        if (filteredList.size() > 0 && item.isNoted()) {
                            itemCount += filteredList.get(0).getAmount();
                        }
                        else { // increase count by the number of occurrences
                            itemCount += filteredList.size();
                        }

                        // remove the charge
                        while (itemName.charAt(openIndex + 1) != ')') {
                            itemName.deleteCharAt(openIndex + 1);
                        }
                        // insert the next charge
                        itemName.insert(openIndex + 1, String.valueOf(charge + ++x));

                        // update the filter for the next item check
                        filteredList = itemList.stream()
                                .filter(i -> (i != null && i.getName().toLowerCase().equals(itemName.toString().toLowerCase())))
                                .collect(Collectors.toList());

                    } while (x <= item.getRange());

                    // add to list the missing amount of items
                    if (itemCount < desiredAmount) {
                        missingItems.put(item.getName(), desiredAmount - itemCount);
                    }
                }
                else { // item has no charge, so we check for exact item name
                    int currentAmount = 0;
                    // the item is stackable or we only have 1 occurrence, so get the amount
                    if (filteredList.size() == 1) {
                        currentAmount = filteredList.get(0).getAmount();
                    }
                    else { // we have more than 1 occurrence, so the item probably isn't stackable
                        currentAmount = filteredList.size();
                    }
                    // if insufficient amount, add to list the item
                    // and the missing amount
                    if (currentAmount < desiredAmount) {
                        missingItems.put(item.getName(), desiredAmount - currentAmount);
                    }
                }
            }
            else { // we don't have the item, so we're missing all
                missingItems.put(item.getName(), desiredAmount);
            }
        }

        return missingItems;
    }

    /**
     * @param item the item's name
     *
     * @return the number of charges/doses the item has. Returns 0 if no
     *         charge/dose.
     */
    private int getItemCharge(String item) {
        int openIndex = item.indexOf("(");
        int closeIndex = item.indexOf(")");
        int charge = 0;

        if (openIndex != -1 && closeIndex != -1) {
            try {
                charge = Integer.parseInt(item.substring(openIndex + 1, closeIndex));
            } catch (NumberFormatException e) {

            }
        }
        return charge;
    }

    public static class RequiredItem {

        private String name;
        private int amount;
        private int range;
        private boolean noted;

        /**
         * Represents a required, single, un-noted item matching only this
         * charge/dose, if it has one.
         *
         * @param name the name of the item
         */
        public RequiredItem(String name) {
            this(name, 1, 0, false);
        }

        /**
         * Represents a required, un-noted item, of at least the specified quantity,
         * matching only this charge/dose, if it has one.
         *
         * @param name the name of the item
         * @param amount the amount of the item
         */
        public RequiredItem(String name, int amount) {
            this(name, amount, 0, false);
        }

        /**
         * Represents a required, un-noted item, of at least the specified quantity,
         * matching this item's charge/dose, if it has one, up to an additional
         * value of the range.
         *
         * @param name the name of the item
         * @param amount the amount of the item
         * @param range the additional charges/doses to accept
         */
        public RequiredItem(String name, int amount, int range) {
            this(name, amount, range, false);
        }

        /**
         * Represents a required, noted item, of at least the specified quantity,
         * matching this item's charge/dose, if it has one, up to an additional
         * value of the range.
         *
         * @param name the name of the item
         * @param amount the amount of the item
         * @param range the additional charges/doses to accept
         * @param noted whether or not the item is noted
         */
        public RequiredItem(String name, int amount, int range, boolean noted) {
            this.name = name;
            this.amount = amount;
            this.range = range;
            this.noted = noted;
        }

        public String getName() {
            return name;
        }

        public int getAmount() {
            return amount;
        }

        public int getRange() {
            return range;
        }

        public boolean isNoted() {
            return noted;
        }
    }
}
