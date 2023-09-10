package zc246_zl345_co232_mw756;

import edu.cornell.cs.cs4120.util.CodeWriterSExpPrinter;
import edu.cornell.cs.cs4120.util.SExpPrinter;
import edu.cornell.cs.cs4120.xic.ir.*;
import edu.cornell.cs.cs4120.xic.ir.interpret.Cli;
import edu.cornell.cs.cs4120.xic.ir.interpret.IRSimulator;
import edu.cornell.cs.cs4120.xic.ir.visit.BasicBlock;
import edu.cornell.cs.cs4120.xic.ir.visit.ConstFolding;
import edu.cornell.cs.cs4120.xic.ir.visit.LIR;
import edu.cornell.cs.cs4120.xic.ir.visit.SeqRemover;
import java_cup.runtime.Scanner;
import java_cup.runtime.Symbol;
import java_cup.runtime.lr_parser;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import polyglot.util.OptimalCodeWriter;
import zc246_zl345_co232_mw756.assembly.*;
import zc246_zl345_co232_mw756.ast.Node;
import zc246_zl345_co232_mw756.ast.Program;
import zc246_zl345_co232_mw756.errors.CompilerError;
import zc246_zl345_co232_mw756.lex.YyLex;
import zc246_zl345_co232_mw756.opt.*;
import zc246_zl345_co232_mw756.visitor.IRGenerator;
import zc246_zl345_co232_mw756.visitor.TypeChecker;

import java.io.*;
import java.util.*;

public class Main {
    static Node pr;
    static boolean O = true, cf = false, cp = false, copy = false, dce = false, reg = false, vn = false,
            ir_initial = false, ir_final = false, cfg_initial = false, cfg_final = false;
    static BufferedWriter lexWriter, typeWriter, irWriter, aaWriter, asmWriter;
    static OutputStream parseWriter;

    public static void main(String[] args) {
        Options options = new Options();
        options.addOption(Option.builder().longOpt("help")
                .desc("Print this help message")
                .build());
        options.addOption(Option.builder().longOpt("lex")
                .desc("Generate output from lexical analysis")
                .build());
        options.addOption(Option.builder().longOpt("parse")
                .desc("Generate output from syntactic analysis")
                .build());
        options.addOption(Option.builder().longOpt("typecheck")
                .desc("Generate output from semantic analysis")
                .build());
        options.addOption(Option.builder().longOpt("irgen")
                .desc("Generate intermediate code")
                .build());
        options.addOption(Option.builder().longOpt("irrun")
                .desc("Generate and interpret intermediate code")
                .build());
        options.addOption(Option.builder().longOpt("aa")
                .desc("Generate abstract assembly")
                .build());
        options.addOption(Option.builder().longOpt("report-opts")
                .desc("Output a list of optimizations supported by the compiler")
                .build());
        options.addOption(Option.builder().longOpt("optir")
                .desc("Report the intermediate code at the specified phase of optimization")
                .hasArg()
                .build());
        options.addOption(Option.builder().longOpt("optcfg")
                .desc("Report the control-flow graph at the specified phase of optimization")
                .hasArg()
                .build());
        options.addOption(Option.builder("D")
                .desc("Specify where to place generated diagnostic files")
                .hasArg()
                .argName("path")
                .build());
        options.addOption(Option.builder("d")
                .desc("Specify where to place generated assembly output files")
                .hasArg()
                .argName("path")
                .build());
        options.addOption(Option.builder("sourcepath")
                .desc("Specify where to find input source files ")
                .hasArg()
                .argName("path")
                .build());
        options.addOption(Option.builder("libpath")
                .desc("Specify where to find library interface files ")
                .hasArg()
                .argName("path")
                .build());
        options.addOption(Option.builder("O")
                .desc("Disable optimizations")
                .build());
        options.addOption(Option.builder("target")
                .desc("Specify the operating system for which to generate code")
                .hasArg()
                .argName("OS")
                .build());
        options.addOption(Option.builder("Oreg")
                .desc("Register allocation")
                .build());
        options.addOption(Option.builder("Ocf")
                .desc("Constant folding")
                .build());
        options.addOption(Option.builder("Ocopy")
                .desc("Copy propagation")
                .build());
        options.addOption(Option.builder("Odce")
                .desc("Dead code elimination")
                .build());
        options.addOption(Option.builder("Ocp")
                .desc("Constant propagation")
                .build());
        options.addOption(Option.builder("Ovn")
                .desc("Value numbering")
                .build());

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine line = parser.parse(options, args);
            String[] files = line.getArgs();
            try {
                if (line.hasOption("report-opts")) {
                    System.out.println("reg");
                    System.out.println("cf");
                    System.out.println("copy");
                    System.out.println("dce");
                    System.out.println("cp");
                    System.out.println("vn");
                } else if (line.hasOption("help") || files.length == 0) {
                    HelpFormatter formatter = new HelpFormatter();
                    formatter.printHelp("./etac [options] <source files>", options);
                } else {
                    setBools(line);
                    String lib_dir = (line.hasOption("libpath") ? line.getOptionValue("libpath") + "/" : "");
                    for (String source_file : files) {
                        try {
                            String name = source_file.split("\\.(?=[^\\.]+$)")[0];
                            String ext = source_file.split("\\.(?=[^\\.]+$)")[1];
                            String input_dir = (line.hasOption("sourcepath") ? line.getOptionValue("sourcepath") + "/" : "") + source_file;
                            boolean out = line.hasOption("lex") || line.hasOption("parse") || line.hasOption("typecheck") || line.hasOption("irgen") || line.hasOption("irrun");
                            String output_dir = null;
                            File file;
                            String assembly_dir = (line.hasOption("d") ? line.getOptionValue("d") + "/" : "") + name;
                            if (out) {
                                output_dir = (line.hasOption("D") ? line.getOptionValue("D") + "/" : "") + name;
                                if (line.hasOption("lex")) {
                                    lexWriter = new BufferedWriter(new FileWriter(output_dir + ".lexed"));
                                }
                                if (line.hasOption("parse")) {
                                    parseWriter = new BufferedOutputStream(new FileOutputStream(output_dir + ".parsed"));
                                }
                                if (line.hasOption("typecheck")) {
                                    typeWriter = new BufferedWriter(new FileWriter(output_dir + ".typed"));
                                }
                                if (line.hasOption("aa")){
                                    aaWriter = new BufferedWriter(new FileWriter(assembly_dir + "_abstract.s"));
                                }
                                if(line.hasOption("irgen") || line.hasOption("irrun")) {
                                    irWriter = new BufferedWriter(new FileWriter(output_dir + ".ir"));
                                }
                                file = new File(output_dir + "." + ext);
                                if (file.getParentFile() != null && !file.getParentFile().exists())
                                    file.getParentFile().mkdirs();
                            }

                            java_cup.runtime.lr_parser lp, p;
                            int EOF;
                            if (ext.equals("eta") || ext.equals("eti")) {
                                lp = new zc246_zl345_co232_mw756.eta.parser(new YyLex(new BufferedReader(new FileReader(input_dir)), false));
                                p = new zc246_zl345_co232_mw756.eta.parser(new YyLex(new BufferedReader(new FileReader(input_dir)), false));
                                EOF = zc246_zl345_co232_mw756.eta.sym.EOF;
                            } else {
                                lp = new zc246_zl345_co232_mw756.rho.parser(new YyLex(new BufferedReader(new FileReader(input_dir)), true));
                                p = new zc246_zl345_co232_mw756.rho.parser(new YyLex(new BufferedReader(new FileReader(input_dir)), true));
                                EOF = zc246_zl345_co232_mw756.rho.sym.EOF;
                            }
                            boolean check = (LexicalCheck(lp, source_file, EOF)
                                    && SyntaxCheck(p, ext, output_dir+".parsed", source_file))
                                    && SemanticCheck(source_file, lib_dir)
                                    ;
                            if(!check || ext.equals("eti") || ext.equals("ri")) continue;

                            IRCompUnit irCompUnit = GenerateIR(source_file, name);
                            if (line.hasOption("irrun")) Cli.main(new String[]{output_dir + ".ir"});

                            if (line.hasOption("target")) {
                                switch (line.getOptionValue("target")) {
                                    case "linux":
                                        break;
                                    case "windows":
                                    case "macos":
                                        throw new Exception("Operating System not supported yet");
                                    default:
                                        throw new Exception("Not a Valid Operating System");
                                }
                            }

                            asmWriter = new BufferedWriter(new FileWriter(assembly_dir + ".s"));
                            file = new File(assembly_dir + ".s");
                            if (file.getParentFile() != null && !file.getParentFile().exists())
                                file.getParentFile().mkdirs();
                            GenerateAssembly(irCompUnit);
                        } catch (IRSimulator.Trap e) {
                            System.err.println("Internal compiler error. Reason: " + e.getMessage());
                        }
                    }
                }
            } catch (IOException exp) {
                System.err.println("File access denied. Reason: " + exp.getMessage());
            }
        } catch (Throwable exp) {
            exp.printStackTrace();
            System.out.println("Exception occurred. Reason: " + exp.getMessage());
        }
    }

    private static boolean LexicalCheck(lr_parser p, String source_file, int EOF) throws Exception {
        Scanner sc = p.getScanner();
        while (true) {
            try {
                Symbol s = sc.next_token();
                if (s.sym == EOF) break;
                if (lexWriter != null) lexWriter.write(Lexer.stringOfToken(s));
            } catch (CompilerError exp) {
                if (lexWriter != null) {
                    lexWriter.write(exp.getMessage() + "\n");
                    lexWriter.close();
                }
                System.out.println(exp.getMessage(source_file));
                return false;
            }
        }
        if(lexWriter != null) lexWriter.close();
        return true;
    }

    private static boolean SyntaxCheck(lr_parser p, String ext, String output_dir, String source_file) throws Exception {
        SExpPrinter printer = null;
        if(parseWriter != null) printer = new CodeWriterSExpPrinter(new OptimalCodeWriter(parseWriter, 80));
        try {
            pr = (Node) p.parse().value;
            if (ext.equals("eta") || ext.equals("rh")) {
                Parser.assertProgram(pr);
            } else {
                Parser.assertInterface(pr);
            }
            if (parseWriter!=null) pr.printNode(printer);
        } catch (CompilerError exp) {
            if (parseWriter != null) {
                BufferedWriter writer = new BufferedWriter(new FileWriter(output_dir));
                writer.write(exp.getMessage() + "\n");
                writer.close();
            }
            if (typeWriter != null) {
                typeWriter.write(exp.getMessage() + "\n");
                typeWriter.close();
            }
            System.out.println(exp.getMessage(source_file));
            return false;
        }
        if (parseWriter != null) {
            printer.flush();
            printer.close();
            parseWriter.close();
        }
        return true;
    }

    private static boolean SemanticCheck(String source_file, String lib_dir) throws Exception {
        try {
            String source = source_file.substring(source_file.lastIndexOf("/") + 1, source_file.lastIndexOf("."));
            HashMap<String, Program> intfs = new HashMap<>();
            String[] result = getIntf(lib_dir, intfs);
            if(result != null){
                if (typeWriter != null) typeWriter.write(result[0] + "\n");
                System.out.println(result[1]);
                return false;
            }
            TypeChecker tc = new TypeChecker(intfs, source);
            if (pr instanceof Program) {
                tc.visitProgram((Program) pr);
                if (typeWriter != null) {
                    typeWriter.write("Valid Eta Program\n");
                }
            }
        } catch (CompilerError exp) {
            if (typeWriter != null) {
                typeWriter.write(exp.getMessage() + "\n");
                typeWriter.close();
            }
            System.out.println(exp.getMessage(source_file));
            return false;
        }
        if (typeWriter != null) typeWriter.close();
        return true;
    }

    private static IRCompUnit GenerateIR(String source_file, String pathToFile) throws Exception {
        String filename = source_file.substring(source_file.lastIndexOf("/") + 1);
        IRGenerator irgen = new IRGenerator();
        IRCompUnit compUnit = irgen.visitProgram((Program) pr, filename);
        LIR lir = new LIR(new IRNodeFactory_c());
        compUnit = (IRCompUnit) lir.visit(compUnit);
        SeqRemover sr = new SeqRemover();
        sr.visit(compUnit);
        BasicBlock bb = new BasicBlock(compUnit);
        compUnit = bb.createBlocks();

        if (ir_initial) {
            irPrint(compUnit, pathToFile + "_initial.ir");
        }
        if (O || cf) {
            ConstFolding constFolding = new ConstFolding(new IRNodeFactory_c());
            compUnit = (IRCompUnit) constFolding.visit(compUnit);
        }
        if (irWriter != null) {
            irWriter.write(IRPrettyPrint(compUnit) + "\n");
            irWriter.close();
        }
        if(O||vn) compUnit = bb.valueNumbering();
        compUnit = irOpt(compUnit, pathToFile);
        if (ir_final) {
            irPrint(compUnit, pathToFile + "_final.ir");
        }
        return compUnit;
    }

    private static void GenerateAssembly(IRCompUnit irCompUnit) throws Exception {
        Tiling tile = new Tiling(false);
        AACompUnit aaCompUnit = tile.tileCompUnit(irCompUnit);

        if(aaWriter!=null) {
            aaWriter.write(aaCompUnit.printer() + "\n");
            aaWriter.close();
        }
        if (O || reg) {
            for(Map.Entry<String, Function> entry : aaCompUnit.getFunctions().entrySet()){
                Function f = entry.getValue();
                CFG<Assembly> cfg = CFG.makeCFG(f.getBody());

                RegisterAllocation ra = new RegisterAllocation(cfg);
                Const enter = ra.Main();
                CFG<Assembly> optimized = ra.getCFG();
                List<Assembly> codes = optimized.toCode();
                codes.add(0, new Enter(enter));
                aaCompUnit.getFunctions().put(entry.getKey(), new Function(f.getName(), codes));
            }
        }else{
            NaiveRegisterAllocation regAlloc = new NaiveRegisterAllocation();
            aaCompUnit = regAlloc.alloc(aaCompUnit);
        }

        asmWriter.write(aaCompUnit.printer() + "\n");
        asmWriter.close();
    }

    private static String IRPrettyPrint(IRCompUnit compUnit) {
        StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw);
             SExpPrinter sp = new CodeWriterSExpPrinter(pw)) {
            compUnit.printSExp(sp);
        }
        return sw.toString();
    }

    private static IRCompUnit irOpt(IRCompUnit cunit, String pathToFile) throws IOException {
        Map<String, IRFuncDecl> funcs = new LinkedHashMap<>();
        for (Map.Entry<String, IRFuncDecl> entry : cunit.functions().entrySet()) {
            CFG<IRStmt> cfg = CFG.makeCFG(((IRSeq) entry.getValue().body()).stmts());
            String fName = entry.getKey().substring(2, entry.getKey().indexOf("_", 2));
            if (cfg_initial) {
                cfg.toDot(pathToFile + "_" + fName + "_initial.dot");
            }
            for (boolean converged = false; !converged; ) {
                boolean c = true;
                if (O || cp) {
                    ConstProp constProp = new ConstProp(cfg);
                    constProp.run();
                    cfg = constProp.finish();
                    c = constProp.converged;
                }
                if (O || copy) {
                    CopyProp copyProp = new CopyProp(cfg);
                    copyProp.run();
                    cfg = copyProp.finish();
                    cfg.simplify();
                    c = c && copyProp.converged;
                }
                if (O || dce) {
                    DeadCodeElimination deadCodeElimination = new DeadCodeElimination(cfg, cunit.dataMap());
                    deadCodeElimination.run();
                    cfg = deadCodeElimination.finish();
                    c = c && deadCodeElimination.converged;
                }
                converged = c;
            }
            if (cfg_final) {
                cfg.toDot(pathToFile + "_" + fName + "_final.dot");
            }
            funcs.put(entry.getKey(), new IRFuncDecl(entry.getValue().name(), new IRSeq(cfg.toCode())));
        }
        return new IRCompUnit(cunit.name(), funcs, cunit.ctors(), cunit.dataMap());
    }

    private static void irPrint(IRCompUnit cunit, String path) throws IOException {
        String prettyPrintedProgram = IRPrettyPrint(cunit);
        BufferedWriter writer = new BufferedWriter(new FileWriter(path));
        writer.write(prettyPrintedProgram + "\n");
        writer.close();
    }

    private static void setBools(CommandLine line) {
        if (line.hasOption("O")) {
            O = false;
        }
        if (line.hasOption("Ocf")) {
            cf = true;
            O = false;
        }
        if (line.hasOption("Ocp")) {
            cp = true;
            O = false;
        }
        if (line.hasOption("Ocopy")) {
            copy = true;
            O = false;
        }
        if (line.hasOption("Odce")) {
            dce = true;
            O = false;
        }
        if (line.hasOption("Oreg")) {
            reg = true;
            O = false;
        }
        if (line.hasOption("Ovn")) {
            vn = true;
            O = false;
        }
        if (line.hasOption("optir")) {
            String[] args = line.getOptionValues("optir");
            if (args.length == 2) {
                ir_initial = true;
                ir_final = true;
            } else if (args[0].equals("initial")) {
                ir_initial = true;
            } else if (args[0].equals("final")) {
                ir_final = true;
            }
        }
        if (line.hasOption("optcfg")) {
            String[] args = line.getOptionValues("optcfg");
            if (args.length == 2) {
                cfg_initial = true;
                cfg_final = true;
            } else if (args[0].equals("initial")) {
                cfg_initial = true;
            } else if (args[0].equals("final")) {
                cfg_final = true;
            }
        }
    }

    private static String[] getIntf(String lib_dir, HashMap<String, Program> map) throws Exception {
        File dir = new File(lib_dir);
        String[] extensions = new String[]{"eti", "ri"};
        List<File> intfs = (List<File>) FileUtils.listFiles(dir, extensions, false);
        for (File intf : intfs) {
            try {
                java_cup.runtime.lr_parser parser_intf;
                if (intf.getName().endsWith("eti")) {
                    parser_intf = new zc246_zl345_co232_mw756.eta.parser(new YyLex(new BufferedReader(new FileReader(intf)), false));
                } else {
                    parser_intf = new zc246_zl345_co232_mw756.rho.parser(new YyLex(new BufferedReader(new FileReader(intf)), true));
                }
                Node p = (Node) parser_intf.parse().value;
                String fname = intf.getName().split("\\.(?=[^\\.]+$)")[0];
                map.put(fname, (Program) p);
            }catch (CompilerError e){
                return new String[]{e.getMessage(), e.getMessage(intf.getPath())};
            }
        }
        return null;
    }


}