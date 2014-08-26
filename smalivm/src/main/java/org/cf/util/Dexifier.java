package org.cf.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.TokenSource;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.apache.commons.io.FileUtils;
import org.jf.dexlib2.writer.builder.BuilderClassDef;
import org.jf.dexlib2.writer.builder.DexBuilder;
import org.jf.smali.LexerErrorInterface;
import org.jf.smali.smaliFlexLexer;
import org.jf.smali.smaliParser;
import org.jf.smali.smaliTreeWalker;

public class Dexifier {

    private static final Logger log = Logger.getLogger(Dexifier.class.getSimpleName());

    public static final int DEFAULT_API_LEVEL = 15;

    public static List<BuilderClassDef> dexifySmaliFiles(String path) throws Exception {
        DexBuilder dexBuilder = DexBuilder.makeDexBuilder(Dexifier.DEFAULT_API_LEVEL);

        return dexifySmaliFiles(path, dexBuilder);
    }

    public static List<BuilderClassDef> dexifySmaliFiles(String path, DexBuilder dexBuilder) throws Exception {
        List<File> smaliFiles;
        File f = new File(path);
        if (f.isDirectory()) {
            smaliFiles = (List<File>) FileUtils.listFiles(f, new String[] { "smali" }, true);
        } else {
            smaliFiles = new ArrayList<File>();
            smaliFiles.add(f);
        }

        return dexifySmaliFiles(smaliFiles, dexBuilder);
    }

    public static List<BuilderClassDef> dexifySmaliFiles(List<File> smaliFiles, DexBuilder dexBuilder) throws Exception {
        List<BuilderClassDef> result = new ArrayList<BuilderClassDef>();
        for (File smaliFile : smaliFiles) {
            result.add(dexifySmaliFile(smaliFile, dexBuilder));
        }

        return result;
    }

    public static BuilderClassDef dexifySmaliFile(File smaliFile, DexBuilder dexBuilder) throws Exception {
        log.info("Dexifying: " + smaliFile);

        FileInputStream fis = new FileInputStream(smaliFile.getAbsolutePath());
        InputStreamReader reader = new InputStreamReader(fis, "UTF-8");

        LexerErrorInterface lexer = new smaliFlexLexer(reader);
        ((smaliFlexLexer) lexer).setSourceFile(smaliFile);
        CommonTokenStream tokens = new CommonTokenStream((TokenSource) lexer);

        smaliParser parser = new smaliParser(tokens);
        parser.setApiLevel(DEFAULT_API_LEVEL);

        smaliParser.smali_file_return result = parser.smali_file();
        if ((parser.getNumberOfSyntaxErrors() > 0) || (lexer.getNumberOfSyntaxErrors() > 0)) {
            throw new RuntimeException("Unable to parse: " + smaliFile);
        }

        CommonTree t = result.getTree();
        CommonTreeNodeStream treeStream = new CommonTreeNodeStream(t);
        treeStream.setTokenStream(tokens);

        smaliTreeWalker dexGen = new smaliTreeWalker(treeStream);
        dexGen.setVerboseErrors(false);
        dexGen.setDexBuilder(dexBuilder);
        BuilderClassDef classDef = (BuilderClassDef) dexGen.smali_file();
        if (dexGen.getNumberOfSyntaxErrors() != 0) {
            throw new RuntimeException("Unable to walk: " + smaliFile);
        }

        return classDef;
    }

}
