package org.aegisguard.android;

public class TestCanYouBlockIt {
    public static void main(String[] args) {
        String[] domains = {
            "taboola.com",
            "cdn.taboola.com",
            "trc.taboola.com",
            "outbrain.com",
            "widgets.outbrain.com",
            "criteo.com",
            "static.criteo.net",
            "popads.net",
            "popcash.net",
            "media.net",
            "static.media.net",
            "zergnet.com",
            "revcontent.com",
            "mgid.com",
            "propellerads.com",
            "adsterra.com",
            "exoclick.com",
            "juicyads.com",
            "hilltopads.com",
            "trafficjunky.com",
            "infolinks.com",
            "bidvertiser.com",
            "adnxs.com",
            "rubiconproject.com",
            "openx.net",
            "pubmatic.com"
        };

        System.out.println("CAN-YOU-BLOCK-IT & TOP AD NETWORKS:");
        int blocked = 0;
        for (String d : domains) {
            FilterEngine.MatchResult res = FilterEngine.evaluate(d);
            boolean b = res.isBlocked();
            if (b) blocked++;
            System.out.printf("  %-25s : %s\n", d, (b ? "BLOCKED" : "ACCESSIBLE"));
        }
        System.out.println("Total: " + blocked + "/" + domains.length);
    }
}
