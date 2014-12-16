import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: denise
 * Date: 12/14/14
 * Time: 4:35 PM
 */
public class Convert {

    public String run(String inString) {
        Pattern p = Pattern.compile("\\$([^\\$]*)\\$");
        Matcher m = p.matcher(inString);


        StringBuffer stringBuffer = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(stringBuffer, String.format("<div class=\"katex\">%s</div>", m.group(1)));
        }
        m.appendTail(stringBuffer);

        return stringBuffer.toString();
    }

    public static void main(String[] args) {
        Convert c = new Convert();
        String outString = c.run("<p>I recently encountered an IMO question that&#8217;s stated as follows: Given a function $f: \\\\mathbb{R} \\\\rightarrow \\\\mathbb{R}$  such that for any $x, y \\\\in \\\\mathbb{R}$, $f(x + y) \\\\leq yf(x) + f(f(x))$, show that $f(x) = 0$  for all $x \\\\leq 0$.</p>\n" +
                "<p>There are two solutions, and I will present the one that AM and I came up with: one can easily verify that $f(x) \\\\leq f(f(x))$. Let $f^2(x) = f(f(x))$. We will proceed by showing the following:</p>\n" +
                "<ol>\n" +
                "<li>$f(x) \\\\leq f^2(0)$</li>\n" +
                "<li>$f(x) \\\\leq 0$</li>\n" +
                "<li>$f^2(0) = 0$</li>\n" +
                "</ol>\n" +
                "<p>At which point the result should be fairly straight-forward.</p>\n" +
                "<p>To show (1), consider $f(f(x)) = f(f(x) + 0) \\\\leq f(x)f(0) + f^2(0)$. From this, we have that $f(x &#8211; f(0)) \\\\leq -f(0)f(x) + f^2(x) \\\\leq -f(0)f(x) + f(0)f(x) + f^2(0) = f^2(0)$. As $x$  is arbitrary, (1) follows.</p>\n" +
                "<p>For (2), let $y > 0$, and consider $f(x &#8211; y + y) \\\\leq yf(x &#8211; y) + f^2(x + y)$. As $y$ is positive, we have that $yf(x &#8211; y) \\\\leq -y^2f(x) + yf(f(x))$, so $f(x) \\\\leq -y^2f(x) + yf^2(x) + f^2(x + y)$. Via (1), we have that $f(f(x)) \\\\leq f^2(0)$, $f^2(x + y) \\\\leq f^2(0)$, which in turn implies that $f(x) \\\\leq -y^2f(x) + (1 + y)f^2(0)$.</p>\n" +
                "<p>But this implies $(1 + y^2)f(x) \\\\leq (1 + y)f^2(0)$, which in turn implies that<br />\n" +
                "$$<br />\n" +
                "f(x) \\\\leq \\\\frac{1 + y}{1 + y^2}f^2(0).<br />\n" +
                "$$<br />\n" +
                "As $y$ is arbitrary, we have that $f(x) \\\\leq 0$: this proves (2).</p>\n" +
                "<p>To prove (3), notice that for any $x$, $f(x) = f(x &#8211; 1 + 1) \\\\leq f(x &#8211; 1) + f(f(x &#8211; 1))$, and use (1). That is $2f^2(0) \\\\geq f(x)$ for any $x$ &#8212; in particular, $x = f(0)$. Together with (2), we have $f^2(0) = 0$.</p>\n" +
                "<p>Finally, as $f^2(0) \\\\leq f^3(0) = f(0)$, we have that $f(0) = 0$; now observe $0 = f(0) = f(x &#8211; x) \\\\leq -xf(x) + f(f(x))$. The main result follows.</p>\n" +
                "<div style=\"width: 12px; height: 12px; position: relative; right: 0px; background-color: black\"></div>");

        System.out.println(outString);
    }
}
