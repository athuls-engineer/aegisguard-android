package org.aegisguard.android;

public class TestYoutubeMorpheDomains {
    public static void main(String[] args) {
        String[] domains = {
            "s.youtube.com",
            "s2.youtube.com",
            "video-stats.l.google.com",
            "video-stats.video.google.com",
            "video-stats.googlevideo.com",
            "www.youtube.com",
            "m.youtube.com",
            "youtubei.googleapis.com",
            "redirector.googlevideo.com",
            "sponsor.ajay.app",
            "sponsor.ajay.app",
            "returnyoutubedislikeapi.com",
            "jnn-pa.googleapis.com",
            "mtalk.google.com",
            "firebaseinstallations.googleapis.com",
            "fcmregistrations.googleapis.com",
            "ads.youtube.com",
            "ad.youtube.com",
            "youtubeads.googleapis.com",
            "youtubei-ad.googleapis.com",
            "ad-delivery.youtube.com",
            "ssl.p.jwpcdn.com",
            "prd.jwpltx.com",
            "g.jwpsrv.com"
        };

        System.out.println("YOUTUBE / MORPHE / REVANCED DOMAINS EVALUATION:");
        for (String d : domains) {
            FilterEngine.MatchResult res = FilterEngine.evaluate(d);
            System.out.printf("  %-40s : %s (%s | %s)\n", d, (res.isBlocked() ? "BLOCKED" : "CLEAN"), res.category, res.rule);
        }
    }
}
