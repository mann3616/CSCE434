package pl434;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

// You need to put jar files in lib/ in your classpath
import org.apache.commons.cli.*;

public class CompilerTester {

    public static void main(String[] args) {
        Options options = new Options();
        options.addRequiredOption("s", "src", true, "Source File");
        options.addOption("i", "in", true, "Data File");
        options.addOption("nr", "reg", true, "Num Regs");
        options.addOption("b", "asm", false, "Print DLX instructions");
        options.addOption("o", "opt", true, "Order-sensitive optimization -allowed to have multiple");

        HelpFormatter formatter = new HelpFormatter();
        CommandLineParser cmdParser = new DefaultParser();
        CommandLine cmd = null;
        try {
            cmd = cmdParser.parse(options, args);
        } catch (ParseException e) {
            formatter.printHelp("All Options", options);
            System.exit(-1);
        }

        Scanner s = null;
        String sourceFile = cmd.getOptionValue("src");
        try {
            s = new Scanner(new FileReader(sourceFile));
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error accessing the code file: \"" + sourceFile + "\"");
            System.exit(-3);
        }

        InputStream in = System.in;
        if (cmd.hasOption("in")) {
            String inputFilename = cmd.getOptionValue("in");
            try {
                in = new FileInputStream(inputFilename);
            }

            catch (IOException e) {
                System.err.println("Error accessing the data file: \"" + inputFilename + "\"");
                System.exit(-2);
            }
        }

        String strNumRegs = cmd.getOptionValue("reg", "24");
        int numRegs = 24;
        try {
            numRegs = Integer.parseInt(strNumRegs);
            if (numRegs > 24) {
                System.err.println("reg num too large - setting to 24");
                numRegs = 24;
            }
            if (numRegs < 4) {
                System.err.println("reg num too small - setting to 4");
                numRegs = 4;
            }
        } catch (NumberFormatException e) {
            System.err.println("Error in option NumRegs -- reseting to 24 (default)");
            numRegs = 24;
        }

        Compiler c = new Compiler(s, numRegs);
        int[] program = c.compile();
        if (c.hasError()) {
            System.err.println("Error compiling file");
            System.err.println(c.errorReport());
            System.exit(-4);
        }

        if (cmd.hasOption("asm")) {

            String asmFile = sourceFile.substring(0, sourceFile.lastIndexOf('.')) + "_asm.txt";
            try (PrintStream out = new PrintStream(asmFile)) {
                for (int i = 0; i < program.length; i++) {
                    out.print(i + ":\t" + DLX.instrString(program[i])); // \newline included in DLX.instrString()
                }
            } catch (IOException e) {
                System.err.println("Error accessing the asm file: \"" + asmFile + "\"");
                System.exit(-5);
            }
        }

        DLX.load(program);
        try {
            DLX.execute(in);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("IOException inside DLX");
            System.exit(-6);
        }
    }
}
