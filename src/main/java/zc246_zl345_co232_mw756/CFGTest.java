package zc246_zl345_co232_mw756;

import edu.cornell.cs.cs4120.util.CodeWriterSExpPrinter;
import edu.cornell.cs.cs4120.util.SExpPrinter;
import edu.cornell.cs.cs4120.xic.ir.*;
import edu.cornell.cs.cs4120.xic.ir.interpret.Cli;
import edu.cornell.cs.cs4120.xic.ir.visit.BasicBlock;
import edu.cornell.cs.cs4120.xic.ir.visit.ConstFolding;
import edu.cornell.cs.cs4120.xic.ir.visit.LIR;
import edu.cornell.cs.cs4120.xic.ir.visit.SeqRemover;
import org.apache.commons.io.FileUtils;
import zc246_zl345_co232_mw756.assembly.AACompUnit;
import zc246_zl345_co232_mw756.assembly.Assembly;
import zc246_zl345_co232_mw756.assembly.Function;
import zc246_zl345_co232_mw756.assembly.Tiling;
import zc246_zl345_co232_mw756.ast.Node;
import zc246_zl345_co232_mw756.ast.Program;
import zc246_zl345_co232_mw756.eta.parser;
import zc246_zl345_co232_mw756.lex.YyLex;
import zc246_zl345_co232_mw756.opt.*;
import zc246_zl345_co232_mw756.visitor.IRGenerator;
import zc246_zl345_co232_mw756.visitor.TypeChecker;

import java.io.*;
import java.util.*;

public class CFGTest {
    static Node pr;
    static String filename = "t";
    static String op = "cp";
    static final HashSet<String> IROps = new HashSet<>(Arrays.asList("copy", "cp", "dce"));

    public static void main(String[] args) throws Exception {
        String input_dir = filename+".eta";
        parser p = new parser(new YyLex(new BufferedReader(new FileReader(input_dir)), false));
        pr = (Node) p.parse().value;
        TypeChecker tc = new TypeChecker(getIntf("test/lib/"), "");
        pr.accept(tc);
        IRCompUnit irCompUnit = getCompUnit(input_dir, false);
        if (IROps.contains(op)) {
            IRPrettyPrint(irCompUnit, "t.ir");

            Map<String, IRFuncDecl> funcs = new HashMap<>();
            for (String s : irCompUnit.functions().keySet()) {
                IRFuncDecl f = irCompUnit.functions().get(s);
                List<IRStmt> stmts = ((IRSeq) f.body()).stmts();
                CFG<IRStmt> cfg = CFG.makeCFG(stmts);
                cfg.toDot(String.format("%s%s.dot", filename, f.name()), op, null);
                Runtime.getRuntime().exec(String.format("dot -Tpng %s%s.dot -o %s%s.png", filename, f.name(), filename, f.name()));

                CFG<IRStmt> finished = optimizeIR(cfg, f.name());
                funcs.put(s, new IRFuncDecl(f.name(), new IRSeq(finished.toCode())));
            }

            irCompUnit = new IRCompUnit(irCompUnit.name(), funcs, irCompUnit.ctors(), irCompUnit.dataMap());
            IRPrettyPrint(irCompUnit, String.format("t_%s.ir", op));
            Cli.main(new String[]{String.format("t_%s.ir", op)});
        } else { // register allocation
            Tiling tile = new Tiling(false);
            AACompUnit aaCompUnit = tile.tileCompUnit(irCompUnit);
            for (String s : aaCompUnit.getFunctions().keySet()) {
                Function f = aaCompUnit.getFunctions().get(s);
                List<Assembly> stmts = f.getBody();
                CFG<Assembly> cfg = CFG.makeCFG(stmts);
                DataFlow df = new LiveVariable(cfg);
                df.run();
                CFG<Assembly> finished = df.finish();
                finished.toDot(String.format("%s%s_%s.dot", filename, f.getName(), op), op, df);
                Runtime.getRuntime().exec(String.format("dot -Tpng %s%s_%s.dot -o %s%s_%s.png", filename, f.getName(), op, filename, f.getName(), op));
            }
        }
    }

    private static CFG<IRStmt> optimizeIR(CFG<IRStmt> cfg, String fName) throws IOException {
        DataFlow df = null;
        if (op.equals("copy")) {
            df = new CopyProp(cfg);
        } else if (op.equals("cp")) {
            df = new ConstProp(cfg);
        } else if (op.equals("dce")) {
            df = new DeadCodeElimination(cfg, new HashMap<>());
        }
        df.run();
        CFG<IRStmt> finished = df.finish();
        finished.toDot(String.format("%s%s_%s.dot", filename, fName, op), op, null);
        Runtime.getRuntime().exec(String.format("dot -Tpng %s%s_%s.dot -o %s%s_%s.png", filename, fName, op, filename, fName, op));
        return finished;
    }

    private static void IRPrettyPrint(IRCompUnit compUnit, String name) {
        try (BufferedWriter sw = new BufferedWriter(new FileWriter(name));
             PrintWriter pw = new PrintWriter(sw);
             SExpPrinter sp = new CodeWriterSExpPrinter(pw)) {
            compUnit.printSExp(sp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void GenerateAssembly(IRCompUnit irCompUnit) throws Exception {
        Tiling tile = new Tiling(false);
        AACompUnit aaCompUnit = tile.tileCompUnit(irCompUnit);
        for(String s : aaCompUnit.getFunctions().keySet()){
            Function f = aaCompUnit.getFunctions().get(s);
            CFG<Assembly> assemblyCFG = CFG.makeCFG(f.getBody());
            assemblyCFG.toDot(String.format("%s%s.dot", filename, s));
            Runtime.getRuntime().exec(String.format("dot -Tpng %s%s.dot -o %s%s.png", filename, s, filename, s));
            zc246_zl345_co232_mw756.assembly.BasicBlock bb = new zc246_zl345_co232_mw756.assembly.BasicBlock();
            zc246_zl345_co232_mw756.assembly.BasicBlock.Block b =  bb.makeBasicBlock(assemblyCFG);
            System.out.println("123");
        }
    }

    private static IRCompUnit getCompUnit(String source_file, boolean o) throws Exception {
        Map<String, IRData> dataMap = new LinkedHashMap<>();
        if (!o) pr.constFold();
        String filename = source_file.substring(source_file.lastIndexOf("/") + 1);
        IRGenerator irgen = new IRGenerator();
        IRCompUnit compUnit = irgen.visitProgram((Program) pr, filename);
        LIR lir = new LIR(new IRNodeFactory_c());
        compUnit = (IRCompUnit) lir.visit(compUnit);
        SeqRemover sr = new SeqRemover();
        sr.visit(compUnit);
        BasicBlock bb = new BasicBlock(compUnit);
        compUnit = bb.createBlocks();
        if (!o) {
            ConstFolding cf = new ConstFolding(new IRNodeFactory_c());
            compUnit = (IRCompUnit) cf.visit(compUnit);
        }
        return compUnit;
    }

    private static HashMap<String, Program> getIntf(String lib_dir) throws Exception {
        HashMap<String, Program> map = new HashMap<>();
        File dir = new File(lib_dir);
        String[] extensions = new String[]{"eti", "ri"};
        List<File> intfs = (List<File>) FileUtils.listFiles(dir, extensions, false);
        for (File intf : intfs) {
            java_cup.runtime.lr_parser parser_intf;
            if(intf.getName().endsWith("eti")){
                parser_intf = new zc246_zl345_co232_mw756.eta.parser(new YyLex(new BufferedReader(new FileReader(intf)), false));
            }else{
                parser_intf = new zc246_zl345_co232_mw756.rho.parser(new YyLex(new BufferedReader(new FileReader(intf)), true));
            }
            Node pr = (Node) parser_intf.parse().value;
            String fname = intf.getName().split("\\.(?=[^\\.]+$)")[0];
            map.put(fname, (Program) pr);
        }
        return map;
    }
}