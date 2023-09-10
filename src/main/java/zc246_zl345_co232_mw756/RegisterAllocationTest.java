package zc246_zl345_co232_mw756;

import edu.cornell.cs.cs4120.util.CodeWriterSExpPrinter;
import edu.cornell.cs.cs4120.util.SExpPrinter;
import edu.cornell.cs.cs4120.xic.ir.IRCompUnit;
import edu.cornell.cs.cs4120.xic.ir.IRNodeFactory_c;
import edu.cornell.cs.cs4120.xic.ir.visit.BasicBlock;
import edu.cornell.cs.cs4120.xic.ir.visit.LIR;
import edu.cornell.cs.cs4120.xic.ir.visit.SeqRemover;
import org.apache.commons.io.FileUtils;
import zc246_zl345_co232_mw756.assembly.*;
import zc246_zl345_co232_mw756.ast.Node;
import zc246_zl345_co232_mw756.ast.Program;
import zc246_zl345_co232_mw756.errors.CompilerError;
import zc246_zl345_co232_mw756.rho.parser;
import zc246_zl345_co232_mw756.lex.YyLex;
import zc246_zl345_co232_mw756.opt.CFG;
import zc246_zl345_co232_mw756.opt.LiveVariable;
import zc246_zl345_co232_mw756.opt.RegisterAllocation;
import zc246_zl345_co232_mw756.visitor.IRGenerator;
import zc246_zl345_co232_mw756.visitor.TypeChecker;

import java.io.*;
import java.util.Set;
import java.util.*;

public class RegisterAllocationTest {
    static Node pr;
    static String filename = "t";
    static String op = "reg";

    public static void main(String[] args) throws Exception {
        String input_dir = filename + ".eta";
        parser p = new parser(new YyLex(new BufferedReader(new FileReader(input_dir)), false));
        pr = (Node) p.parse().value;
        HashMap<String, Program> intfs = new HashMap<>();
        String result = getIntf("test/lib/", intfs);
        String source = input_dir.substring(input_dir.lastIndexOf("/") + 1, input_dir.lastIndexOf("."));
        TypeChecker tc = new TypeChecker(intfs, filename);
        pr.accept(tc);
        IRCompUnit irCompUnit = getCompUnit("basic", false);
        Tiling tile = new Tiling(false);
        AACompUnit aaCompUnit = tile.tileCompUnit(irCompUnit);
        Map<String, Function> functions = new HashMap<>();

        for (String s : aaCompUnit.getFunctions().keySet()) {
            Function f = aaCompUnit.getFunctions().get(s);
            List<Assembly> stmts = f.getBody();
            CFG<Assembly> cfg = CFG.makeCFG(stmts);

            LiveVariable df = new LiveVariable(cfg);
            df.run();
            CFG<Assembly> finished = df.finish();
            finished.toDot(String.format("%s%s.dot", filename, f.getName()), op, df);
            Runtime.getRuntime().exec(String.format("dot -Tpng %s%s.dot -o %s%s.png", filename, f.getName(), filename, f.getName()));

            zc246_zl345_co232_mw756.assembly.BasicBlock bb = new zc246_zl345_co232_mw756.assembly.BasicBlock();
            zc246_zl345_co232_mw756.assembly.BasicBlock.Block b = bb.makeBasicBlock(finished);
            List<zc246_zl345_co232_mw756.assembly.BasicBlock.Block> blocks = getBlocks(b);
            RegisterAllocation ra = new RegisterAllocation(finished);

            ra.Main();
            CFG<Assembly> optimized = ra.getCFG();
            List<Assembly> code = optimized.toCode();

            optimized.toDot(String.format("%s%s_%s.dot", filename, f.getName(), op), op, df);
            Runtime.getRuntime().exec(String.format("dot -Tpng %s%s_%s.dot -o %s%s_%s.png", filename, f.getName(), op, filename, f.getName(), op));

            functions.put(s, new Function(f.getName(), code));
        }
        AACompUnit newCompUnit = new AACompUnit(aaCompUnit.getName(), functions, aaCompUnit.getDataMap());
        String prettyPrintedProgram = newCompUnit.printer();
        String assembly_dir = filename + "_" + op + ".s";
        OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(assembly_dir));
        BufferedWriter bufwriter = new BufferedWriter(writer);
        bufwriter.write(prettyPrintedProgram + "\n");
        bufwriter.close();
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

    private static void GenerateAssembly(BufferedWriter bufwriter, IRCompUnit irCompUnit) throws Exception {
        Tiling tile = new Tiling(false);
        AACompUnit aaCompUnit = tile.tileCompUnit(irCompUnit);
        NaiveRegisterAllocation regAlloc = new NaiveRegisterAllocation();
        aaCompUnit = regAlloc.alloc(aaCompUnit);
        String prettyPrintedProgram = aaCompUnit.printer();
        bufwriter.write(prettyPrintedProgram + "\n");
    }

    private static IRCompUnit getCompUnit(String source_file, boolean o) throws Exception {
        String filename = source_file.substring(source_file.lastIndexOf("/") + 1);
        IRGenerator irgen = new IRGenerator();
        IRCompUnit compUnit = irgen.visitProgram((Program) pr, filename);
        LIR lir = new LIR(new IRNodeFactory_c());
        compUnit = (IRCompUnit) lir.visit(compUnit);
        SeqRemover sr = new SeqRemover();
        sr.visit(compUnit);
        BasicBlock bb = new BasicBlock(compUnit);
        compUnit = bb.createBlocks();
        return compUnit;
    }

    private static String getIntf(String lib_dir, HashMap<String, Program> map) throws Exception {
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
                return e.getMessage(intf.getPath());
            }
        }
        return null;
    }

    private static List<zc246_zl345_co232_mw756.assembly.BasicBlock.Block> getBlocks(zc246_zl345_co232_mw756.assembly.BasicBlock.Block startBlock) {
        Queue<zc246_zl345_co232_mw756.assembly.BasicBlock.Block> queue = new LinkedList<>(Arrays.asList(startBlock));
        Set<zc246_zl345_co232_mw756.assembly.BasicBlock.Block> visited = new LinkedHashSet<>();
        while (queue.size() > 0) {
            zc246_zl345_co232_mw756.assembly.BasicBlock.Block head = queue.poll();
            if (visited.contains(head)) {
                continue;
            }
            visited.add(head);
            queue.addAll(head.getNext());
        }
        ArrayList<zc246_zl345_co232_mw756.assembly.BasicBlock.Block> blocks = new ArrayList<>();
        for (zc246_zl345_co232_mw756.assembly.BasicBlock.Block b : visited) {
            if (b.getBlock().size() > 0)
                blocks.add(b);
        }
        return blocks;
    }
}