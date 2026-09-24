package de.mik.kabuproxy.digikabu.parser;

import de.mik.kabuproxy.digikabu.DigikabuException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads "Max Muster (WIT12A)" from the navbar of any logged-in page.
 */
public final class HeaderParser
{
    private static final Pattern NAME_AND_CLASS = Pattern.compile("^(.*?)\\s*\\(([^()]+)\\)\\s*$");

    private HeaderParser()
    {
    }

    public static ParsedHeader parse(String html) throws DigikabuException.ParseFailed
    {
        Element brand = Jsoup.parse(html).selectFirst("span.navbar-brand");
        if (brand == null)
        {
            throw new DigikabuException.ParseFailed("navbar with name/class not found", null, html);
        }
        Matcher matcher = NAME_AND_CLASS.matcher(brand.text());
        if (!matcher.matches())
        {
            throw new DigikabuException.ParseFailed("navbar text not in 'Name (Klasse)' format: " + brand.text(), null, html);
        }
        return new ParsedHeader(matcher.group(1).trim(), matcher.group(2).trim());
    }
}
