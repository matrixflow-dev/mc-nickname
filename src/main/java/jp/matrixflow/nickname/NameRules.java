package jp.matrixflow.nickname;

import java.util.Collection;
import java.util.Locale;

/** Decides whether a requested nickname may be used. No Bukkit state, so it can be tested alone. */
final class NameRules {

    /** The outcome: either the nickname to store (null means "remove it") or an error for the player. */
    record Verdict(String nickname, String error) {
        static Verdict ok(String nickname) {
            return new Verdict(nickname, null);
        }

        static Verdict reset() {
            return new Verdict(null, null);
        }

        static Verdict refuse(String error) {
            return new Verdict(null, error);
        }

        boolean refused() {
            return error != null;
        }
    }

    private final int maxLength;
    private final Collection<String> reserved;

    NameRules(int maxLength, Collection<String> reserved) {
        this.maxLength = maxLength;
        this.reserved = reserved;
    }

    /**
     * @param requested what the player typed
     * @param account the account name of the player who gets the nickname
     * @param taken other players' account names and nicknames
     */
    Verdict check(String requested, String account, Collection<String> taken) {
        String name = requested == null ? "" : requested.strip();
        if (name.isEmpty()) {
            return Verdict.refuse("名前を入れてください。");
        }
        if (key(name).equals(key(account))) {
            return Verdict.reset();
        }
        int length = name.codePointCount(0, name.length());
        if (length > maxLength) {
            return Verdict.refuse("名前は" + maxLength + "文字までです。");
        }
        if (!name.codePoints().allMatch(NameRules::allowed)) {
            return Verdict.refuse("名前に使えるのは、文字・数字と _ - ー ・ だけです。");
        }
        for (String r : reserved) {
            if (key(name).equals(key(r))) {
                return Verdict.refuse("「" + name + "」は使えない名前です。");
            }
        }
        for (String t : taken) {
            if (key(name).equals(key(t))) {
                return Verdict.refuse("「" + name + "」はほかの人が使っています。");
            }
        }
        return Verdict.ok(name);
    }

    private static boolean allowed(int c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == 'ー' || c == '・';
    }

    /** Compares names ignoring case and the "." Floodgate puts in front of Bedrock players. */
    static String key(String name) {
        String n = name == null ? "" : name.strip();
        if (n.startsWith(".")) {
            n = n.substring(1);
        }
        return n.toLowerCase(Locale.ROOT);
    }
}
