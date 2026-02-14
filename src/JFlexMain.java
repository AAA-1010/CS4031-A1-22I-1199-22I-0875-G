import java.io.FileReader;

public class JFlexMain {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Usage: java JFlexMain <path-to-file.lang>");
            return;
        }

        ScanReport report = new ScanReport();

        try (FileReader fr = new FileReader(args[0])) {
            Yylex lexer = new Yylex(fr);
            lexer.setReport(report);

            while (true) {
                Token t = lexer.yylex();
                if (t != null && t.getType() == TokenType.EOF) break;
            }
        }

        System.out.print(report.formatFullReport());
    }
}
