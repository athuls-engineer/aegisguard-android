package org.aegisguard.android;

public class TestAdBlockTesterDomains {
    public static void main(String[] args) {
        String[] domains = {
            "pagead2.googlesyndication.com",
            "ymatuhin.ru",
            "an.yandex.ru",
            "www.googletagmanager.com",
            "googletagmanager.com",
            "static.hotjar.com",
            "hotjar.com",
            "mc.yandex.ru",
            "js.sentry-cdn.com",
            "sentry-cdn.com",
            "d2wy8f7a9ursnm.cloudfront.net",
            "browser.sentry-cdn.com",
            "static.cloudflareinsights.com"
        };

        System.out.println("ADBLOCK-TESTER.COM DOMAINS EVALUATION:");
        for (String d : domains) {
            FilterEngine.MatchResult res = FilterEngine.evaluate(d);
            System.out.printf("  %-35s : %s (%s)\n", d, (res.isBlocked() ? "BLOCKED" : "ACCESSIBLE"), res.category);
        }
    }
}
