package ru.vyarus.gradle.plugin.mkdocs.util;

import java.util.Comparator;

/**
 * A {@link Comparator} for version (or chapter) numbers, which have an arbitrary number of decimal points.
 * <p>
 * The code was taken from https://bugs.openjdk.java.net/browse/JDK-8134512 and
 * http://cr.openjdk.java.net/~igerasim/8134512/04/webrev/index.html.
 *
 * @author Ivan Gerasimov
 * @since 07.12.2021
 */
public class VersionsComparator {

    public static VersionsComparator.AlphaDecimalComparator<String> getInstance() {
        return new VersionsComparator.AlphaDecimalComparator<>(Comparator
                .comparing(CharSequence::toString, Comparator.naturalOrder()), false);
    }

    /**
     * Compares char sequences, taking into account their numeric part if one exists.
     */
    public static class AlphaDecimalComparator<T extends CharSequence> implements Comparator<T> {

        private final Comparator<? super CharSequence> alphaComparator;
        private final Comparator<CharSequence> decimalComparator;

        AlphaDecimalComparator(Comparator<? super CharSequence> alphaComparator, boolean leadingZeroesFirst) {
            this(alphaComparator, DecimalComparator.getInstance(leadingZeroesFirst));
        }

        private AlphaDecimalComparator(Comparator<? super CharSequence> alphaComparator,
                                       Comparator<CharSequence> decimalComparator) {
            this.alphaComparator = alphaComparator;
            this.decimalComparator = decimalComparator;
        }

        @Override
        public Comparator<T> reversed() {
            return new AlphaDecimalComparator<>(alphaComparator.reversed(),
                    decimalComparator.reversed());
        }

        @Override
        public int compare(T cs1, T cs2) {
            Decomposer d1 = new Decomposer(cs1);
            Decomposer d2 = new Decomposer(cs2);
            for (; ; ) {
                int cmp;
                if ((cmp = alphaComparator.compare(d1.get(), d2.get())) != 0 ||
                        (cmp = decimalComparator.compare(d1.get(), d2.get())) != 0) {
                    return cmp;
                }
                if (d1.eos() && d2.eos()) return 0;
            }
        }

        /**
         * Given a CharSequence, splits it into a series of subsequences so that
         * every character of the very first subsequence (possibly empty) is
         * not a decimal digit;  then every character of the second subsequence
         * is a decimal digit, and so on.
         */
        private static class Decomposer {
            private final CharSequence sequence;
            private boolean expectingDecimal = false;
            private int index = 0;

            Decomposer(CharSequence sequence) {
                this.sequence = sequence;
            }

            CharSequence get() {
                int start = index, end = start, len = sequence.length() - start;
                while (len > 0) {
                    int cp = Character.codePointAt(sequence, end);
                    int ct = Character.getType(cp);
                    boolean isDecimal = (ct == Character.DECIMAL_DIGIT_NUMBER);
                    if (isDecimal ^ expectingDecimal) {
                        break;
                    }
                    int cpWidth = Character.charCount(cp);
                    end += cpWidth;
                    len -= cpWidth;
                }
                expectingDecimal = !expectingDecimal;
                return sequence.subSequence(start, index = end);
            }

            boolean eos() {
                return index >= sequence.length();
            }
        }
    }

    /**
     * The comparator for comparing character sequences that consist solely
     * of decimal digits.  The result of comparing is as if the values were
     * compared numerically.
     */
    public static class DecimalComparator implements Comparator<CharSequence> {

        private static final Comparator<CharSequence>
                DECIMAL_COMPARATOR_LEADING_ZEROES_FIRST =
                new DecimalComparator(true) {
                    @Override
                    public Comparator<CharSequence> reversed() {
                        return DECIMAL_COMPARATOR_LEADING_ZEROES_FIRST_REVERSED;
                    }
                };

        private static final Comparator<CharSequence>
                DECIMAL_COMPARATOR_LEADING_ZEROES_LAST =
                new DecimalComparator(false) {
                    @Override
                    public Comparator<CharSequence> reversed() {
                        return DECIMAL_COMPARATOR_LEADING_ZEROES_LAST_REVERSED;
                    }
                };

        private static final Comparator<CharSequence>
                DECIMAL_COMPARATOR_LEADING_ZEROES_FIRST_REVERSED =
                new DecimalComparator(true) {
                    @Override
                    public Comparator<CharSequence> reversed() {
                        return DECIMAL_COMPARATOR_LEADING_ZEROES_FIRST;
                    }

                    @Override
                    public int compare(CharSequence cs1, CharSequence cs2) {
                        return super.compare(cs2, cs1);
                    }
                };

        private static final Comparator<CharSequence>
                DECIMAL_COMPARATOR_LEADING_ZEROES_LAST_REVERSED =
                new DecimalComparator(false) {
                    @Override
                    public Comparator<CharSequence> reversed() {
                        return DECIMAL_COMPARATOR_LEADING_ZEROES_LAST;
                    }

                    @Override
                    public int compare(CharSequence cs1, CharSequence cs2) {
                        return super.compare(cs2, cs1);
                    }
                };

        private final boolean leadingZeroesFirst;

        public DecimalComparator(boolean leadingZeroesFirst) {
            this.leadingZeroesFirst = leadingZeroesFirst;
        }

        static Comparator<CharSequence> getInstance(boolean leadingZeroesFirst) {
            return leadingZeroesFirst ? DECIMAL_COMPARATOR_LEADING_ZEROES_FIRST
                    : DECIMAL_COMPARATOR_LEADING_ZEROES_LAST;
        }

        private boolean canSkipLeadingZeroes(CharSequence s, int len) {
            for (int i = 0; i < len; ) {
                int cp = Character.codePointAt(s, i);
                if (Character.digit(cp, 10) != 0)
                    return false;
                i += Character.charCount(cp);
            }
            return true;
        }

        @Override
        public int compare(CharSequence cs1, CharSequence cs2) {
            int len1 = Character.codePointCount(cs1, 0, cs1.length());
            int len2 = Character.codePointCount(cs2, 0, cs2.length());
            int dlen = len1 - len2;
            if (len1 == 0 || len2 == 0) {
                return dlen;
            } else if (dlen > 0) {
                if (!canSkipLeadingZeroes(cs1, dlen))
                    return 1;
                int off = Character.offsetByCodePoints(cs1, 0, dlen);
                cs1 = cs1.subSequence(off, cs1.length());
            } else if (dlen < 0) {
                if (!canSkipLeadingZeroes(cs2, -dlen))
                    return -1;
                int off = Character.offsetByCodePoints(cs2, 0, -dlen);
                cs2 = cs2.subSequence(off, cs2.length());
            }
            int cmp = 0;
            for (int i1 = 0, i2 = 0; i1 < cs1.length(); ) {
                int cp1 = Character.codePointAt(cs1, i1);
                int cp2 = Character.codePointAt(cs2, i2);
                if (cp1 != cp2) {
                    if (cmp == 0) {
                        cmp = cp1 - cp2;
                    }
                    int cmpNum = Character.digit(cp1, 10) -
                            Character.digit(cp2, 10);
                    if (cmpNum != 0) {
                        return cmpNum;
                    }
                }
                i1 += Character.charCount(cp1);
                i2 += Character.charCount(cp2);
            }
            return dlen == 0 ? cmp : (leadingZeroesFirst ^ (dlen < 0) ? -1 : 1);
        }
    }
}
