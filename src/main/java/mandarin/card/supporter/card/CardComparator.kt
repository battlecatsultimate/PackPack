package mandarin.card.supporter.card

class CardComparator : Comparator<Card> {
    override fun compare(o1: Card?, o2: Card?): Int {
        if(o1 == null && o2 == null)
            return 0

        o1 ?: return -1
        o2 ?: return 1

        if (o1.id < 0 && o2.id >= 0)
            return 1

        if (o1.id >= 0 && o2.id < 0)
            return -1

        return if (o1.tier == o2.tier) {
            if (o1.id < 0)
                -o1.id.compareTo(o2.id)
            else
                o1.id.compareTo(o2.id)
        } else {
            o1.tier.ordinal.compareTo(o2.tier.ordinal)
        }
    }
}