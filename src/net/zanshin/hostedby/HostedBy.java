package net.zanshin.hostedby;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HostedBy attempts to locate the domain server name for each of the sites in a
 * Netscape format bookmark file. By examining those domain servers it is hoped
 * one could determine the relative popularity of hosting providers.
 *
 * @author Mark H. Nichols Jun 1, 2008
 *
 */
public class HostedBy {

    /**
     * Main. Reads the file, and processes each line individually.
     *
     * @author Mark H. Nichols May 31, 2008
     *
     * @param args
     */
    public static void main(String[] args) {
        HostedBy thisObj = new HostedBy();
        int readLineCount = 0;
        int domainCount = 0;
        List<String> domains = new ArrayList<String>();
        HashMap<String, Integer> whoIsHosts = new HashMap<String, Integer>();

        long startTime;
        long endTime;

        startTime = System.currentTimeMillis();
        try {
            BufferedReader in = new BufferedReader(new FileReader(
                    "/Users/mark/Projects/java/HostedBy/bookmarks.html"));
            // BufferedReader in = new BufferedReader(new
            // FileReader("/Users/mark/TestBookmarks.html"));
            String str;
            System.out.println("The domains: ");
            while ((str = in.readLine()) != null) {
                readLineCount++;
                String aUrl = thisObj.parseURL(str);
                if (aUrl != null) {
                    domains.add(aUrl);
                    domainCount++;
                    // System.out.println("The parsed URL = " + aUrl);
                    System.out.println(aUrl);
                }
            }
            in.close();
            System.out.println("\nFound " + domainCount + " domains from "
                    + readLineCount + " file lines read.");

            // eliminate duplicates by putting all the domains into a TreeSet
            // not terribly efficient, but it'll work
            Set<String> ts = new TreeSet<String>();
            for (String domain : domains) {
                ts.add(domain);
            }

            domainCount = 0;
            for (String domain : ts) {
                domainCount++;
                System.out.println(domainCount + " " + domain);
            }

            System.out.println("\n" + domainCount
                    + " domains remaining after duplicates removed.");

            if (domainCount == 0) {
                System.out.println("No domains found.  Quiting.");
                return;
            }

            System.out.println("\nLooking up DN Servers...");

            // lookup the domains via whois
            whoIsHosts = thisObj.whoIs(ts);

            System.out.println("\nDomain:Count");

            // output the HashMap sorting it by values
            // pretty inefficient, but it works
            ArrayList<Integer> values = new ArrayList<Integer>();
            values.addAll(whoIsHosts.values());
            Collections.sort(values, Collections.reverseOrder());

            int last_i = -1;
            for (Integer i : values) {
                if (last_i == i) // without duplicates
                    continue;
                last_i = i;
                for (String s : whoIsHosts.keySet()) {
                    if (whoIsHosts.get(s) == i) // which have this value
                        System.out.println(s + ":" + i);
                }
            }

            endTime = System.currentTimeMillis();
            long elapsed = endTime - startTime;
            System.out.println("\nTotal processing time: (milliseconds) "
                    + elapsed);

        } catch (IOException e) {
            System.err.println("Caught IOException: " + e.getMessage());
        }

    }

    /**
     * Parses the passed String looking for a URL. Expects Netscape bookmark
     * file style. However, it should work with anything having an
     * href="http://subdomain.domain.tld/..." format. Your edge cases may vary.
     *
     * @author Mark H. Nichols Jun 1, 2008
     *
     * @param aReadLine
     * @return String
     */
    public String parseURL(String aReadLine) {
        int startIndex = 0;
        int endIndex = 0;

        // System.out.println("aREadLine = " + aReadLine);

        // parse the line for an HREF= and parse the URL from there
        int urlIndex = aReadLine.indexOf("HREF=");

        // System.out.println("urlIndex = " + urlIndex);

        // a -1 means the line hasn't got a HREF instance
        if (urlIndex != -1) {
            // just having an HREF isn't enough. Needs to be http to be of
            // interest
            // (i.e., eliminate feeds and javascript favlets
            int httpIndex = aReadLine.indexOf("http");
            if (httpIndex == -1) {
                System.out
                        .println("Not an http bookmark - skipping further processing.");
                return null;
            }
            // position ourselves at the start of the URL itself
            // HREF="http://
            // .....6
            startIndex = urlIndex + 6;
            endIndex = aReadLine.indexOf("\">");
            String theUrl = aReadLine.substring(startIndex, endIndex);

            // bookmarks include favlets, which are javascript - toss them aside
            // they DO have http in them, the sneaky bastards
            if (theUrl.indexOf("javascript") != -1) {
                System.out.println("Favlet - skipping");
                return null;
            }

            // System.out.println("theUrl = " + theUrl);
            try {
                URI uri = new URI(theUrl);
                // System.out.println("The uri.geHost() = " + uri.getHost());
                // strip any pesky subdomains and return the host
                int firstDot = uri.getHost().indexOf(".");
                int nextDot = uri.getHost().indexOf(".", firstDot + 1);
                if (nextDot != -1) {
                    // there is a second dot, so toss out the characters leading
                    // up and
                    // including the first dot
                    return (uri.getHost().substring(firstDot + 1).toLowerCase());
                }
                else {
                    // only one dot, return the whole uri.getHost() string
                    return (uri.getHost().toLowerCase());
                }

            } catch (URISyntaxException u) {
                System.out.println("Whoops");
                System.err.println("Caught URISyntaxException: "
                        + u.getMessage());
                return null;
            }
        }
        else {
            return null;
        }

    }

    /**
     * Execute the whois command against the domain, displaying only the domain
     * name servers listed. whois <domain> | grep 'Name Server' | head -1
     *
     * @author Mark H. Nichols Jun 1, 2008
     *
     */
    public HashMap<String, Integer> whoIs(Set<String> domains) {
        HashMap<String, Integer> results = new HashMap<String, Integer>();
        File dir = new File("/usr/bin");
        long startTime;
        long endTime;
        String regExpr = "(.*)\\.(.+)\\.(.+)";
        Pattern pat;
        pat = Pattern.compile(regExpr, Pattern.CASE_INSENSITIVE);
        String result;

        for (String domain : domains) {
            String[] cmd = new String[] {
                    "/bin/sh",
                    "-c",
                    "/usr/bin/whois " + domain
                            + " | /usr/bin/grep 'Name Server' | head -1" };

            startTime = System.currentTimeMillis();

            try {
                Runtime rt = Runtime.getRuntime();
                Process p = rt.exec(cmd, null, dir);

                // use StreamGrabber to capture the error and output streams
                StreamGrabber errorGrabber = new StreamGrabber(p
                        .getErrorStream(), "ERROR", false);
                StreamGrabber outputGrabber = new StreamGrabber(p
                        .getInputStream(), "OUTPUT", false);

                // start the grabbers
                Thread et = new Thread(errorGrabber);
                Thread ot = new Thread(outputGrabber);

                et.start();
                ot.start();

                // any non-zero return codes?
                int exitValue = p.waitFor();
                if (exitValue != 0) {
                    System.out.println("Exit value: " + exitValue);
                }

                // use regular expression to parse resultant name to just
                // domain.tld
                // and add it to the HashMap. If an entry already exists in the
                // HashMap, increment
                // its value, i.e., a tally
                Matcher matcher = pat.matcher(outputGrabber.getResult());
                if (matcher.find()) {
                    int groupCount = matcher.groupCount();
                    result = matcher.group(groupCount - 1) + "."
                            + matcher.group(groupCount);
                    if (results.containsKey(result)) {
                        results.put(result, results.get(result) + 1);
                    }
                    else {
                        results.put(result, 1);
                    }
                }
                else {
                    System.out.println("Whois return domain we can't parse: "
                            + outputGrabber.getResult());
                }

                endTime = System.currentTimeMillis();
                System.out.println(domain + " resolved to "
                        + outputGrabber.getResult() + " in "
                        + (endTime - startTime) + " milliseconds.");

            } catch (IOException ioe) {
                System.err.println("Caught IOException: " + ioe.getMessage());
                results = null;
            } catch (Exception e) {
                System.err.println("Caught Exception: processing " + domain
                        + " :" + e.getMessage());
            }

        }
        return results;
    }
}
