package refactor.analysis.transform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.*;
import soot.dava.*;
import soot.dava.toolkits.base.AST.interProcedural.InterProceduralAnalyses;
import soot.dava.toolkits.base.AST.transformations.RemoveEmptyBodyDefaultConstructor;
import soot.dava.toolkits.base.AST.transformations.VoidReturnRemover;
import soot.dava.toolkits.base.misc.PackageNamer;
import soot.grimp.Grimp;
import soot.options.Options;
import soot.tagkit.AnnotationTag;
import soot.tagkit.Tag;
import soot.tagkit.VisibilityAnnotationTag;
import soot.util.Chain;

import java.io.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.GZIPOutputStream;

public class RefactorTransformer extends SceneTransformer {
    private static final Logger logger = LoggerFactory.getLogger(RefactorTransformer.class);

    protected void internalTransform(String phaseName, Map options) {
        Chain<SootClass> applicationClasses = Scene.v().getApplicationClasses();
        for (SootClass sc : applicationClasses) {
            describeSootClass(sc);
        }

        //PaddleHook.v().finishPhases();
        //retrieveAllBodies();
        //ConstantInitializerToTagTransformer.v().transformClass(sc, true);
        PackageNamer.v().fixNames();

        Map<SootMethod, Body> jimpleMethodBodyMapping = new HashMap<SootMethod, Body>();
        for (SootClass sc : applicationClasses) {
            for (SootMethod m : sc.getMethods()) {
                jimpleMethodBodyMapping.put(m, m.getActiveBody());
                m.setActiveBody(Grimp.v().newBody(m.getActiveBody(), "gb"));
                PackManager.v().getPack("gop").apply(m.getActiveBody());
            }
            for (SootMethod m : sc.getMethods()) {
                if (!m.isConcrete()) {
                    continue;
                }
                // all the work done in decompilation is done in DavaBody which
                // is invoked from within newBody
                m.setActiveBody(Dava.v().newBody(m.getActiveBody()));
            }
        }

        postProcessDAVA();

        for (Map.Entry<SootMethod, Body> entry: jimpleMethodBodyMapping.entrySet()) {
            entry.getKey().setActiveBody(entry.getValue());
        }

    }

    private void postProcessDAVA() {
        Chain<SootClass> applicationClasses = Scene.v().getApplicationClasses();

        for (SootClass s : applicationClasses) {
            String fileName = SourceLocator.v().getFileNameFor(s, Options.v().output_format());

            DavaStaticBlockCleaner.v().staticBlockInlining(s);

            // remove returns from void methods
            VoidReturnRemover.cleanClass(s);

            // remove the default constructor if this is the only one present
            RemoveEmptyBodyDefaultConstructor.checkAndRemoveDefault(s);

            logger.debug("Analyzing " + fileName + "... ");

            for (SootMethod m : s.getMethods()) {

                if (m.hasActiveBody()) {
                    DavaBody body = (DavaBody) m.getActiveBody();
                    // System.out.println("body"+body.toString());
                    body.analyzeAST();
                }
            }
        } // going through all classes

        InterProceduralAnalyses.applyInterProceduralAnalyses();

        outputDava();
    }

    private void outputDava() {
        Chain<SootClass> appClasses = Scene.v().getApplicationClasses();

    /*
     * Generate decompiled code
     */
        String pathForBuild = null;
        JarOutputStream jarFile = null;
        ArrayList<String> decompiledClasses = new ArrayList<String>();
        Iterator<SootClass> classIt = appClasses.iterator();
        while (classIt.hasNext()) {
            SootClass s = classIt.next();

            OutputStream streamOut = null;
            PrintWriter writerOut = null;
            String fileName = SourceLocator.v().getFileNameFor(s, Options.output_format_dava);
            decompiledClasses.add(fileName.substring(fileName.lastIndexOf('/') + 1));
            if (pathForBuild == null) {
                pathForBuild = fileName.substring(0, fileName.lastIndexOf('/') + 1);
                // System.out.println(pathForBuild);
            }
            if (Options.v().gzip()) {
                fileName = fileName + ".gz";
            }

            try {
                if (jarFile != null) {
                    JarEntry entry = new JarEntry(fileName.replace('\\', '/'));
                    jarFile.putNextEntry(entry);
                    streamOut = jarFile;
                } else {
                    streamOut = new FileOutputStream(fileName);
                }
                if (Options.v().gzip()) {
                    streamOut = new GZIPOutputStream(streamOut);
                }
                writerOut = new PrintWriter(new OutputStreamWriter(streamOut));
            } catch (IOException e) {
                throw new CompilationDeathException("Cannot output file " + fileName, e);
            }

            logger.debug("Generating " + fileName + "... ");

            G.v().out.flush();

            DavaPrinter.v().printTo(s, writerOut);

            G.v().out.flush();

            {
                try {
                    writerOut.flush();
                    if (jarFile == null) {
                        streamOut.close();
                    }
                } catch (IOException e) {
                    throw new CompilationDeathException("Cannot close output file " + fileName);
                }
            }
        } // going through all classes

    /*
     * Create the build.xml for Dava
     */
        if (pathForBuild != null) {
            // path for build is probably ending in sootoutput/dava/src
            // definetly remove the src
            if (pathForBuild.endsWith("src/")) {
                pathForBuild = pathForBuild.substring(0, pathForBuild.length() - 4);
            }

            String fileName = pathForBuild + "build.xml";

            try {
                OutputStream streamOut = new FileOutputStream(fileName);
                PrintWriter writerOut = new PrintWriter(new OutputStreamWriter(streamOut));
                DavaBuildFile.generate(writerOut, decompiledClasses);
                writerOut.flush();
                streamOut.close();
            } catch (IOException e) {
                throw new CompilationDeathException("Cannot output file " + fileName, e);
            }
        }
    }

    private void describeSootClass(SootClass sc) {
        logger.info("========================= start: " + sc.getName() + " =========================");
        for (SootMethod sm : sc.getMethods()) {
            describeSootMethod(sm);
        }
        logger.info("========================== end: " + sc.getName() + " ==========================");
    }

    private void describeSootMethod(SootMethod sm) {
        logger.debug(sm.getSignature());
        describeTags(sm.getTags());
        describeActiveBody(sm.retrieveActiveBody());
    }

    private void describeActiveBody(Body body) {
        logger.debug(body.toString());
    }

    private void describeTags(List<Tag> tags) {
        for (Tag tag : tags) {
            logger.debug(tag.toString());
            /*
            if (tag instanceof VisibilityAnnotationTag) {
                VisibilityAnnotationTag vTag = (VisibilityAnnotationTag) tag;
                for (AnnotationTag aTag : vTag.getAnnotations()) {
                    logger.debug(tabs + "\ttype: " + getDecentAnnotationType(aTag));
                }
            } else {
                logger.debug(tabs + "\ttype: ???");
            }
            */
        }
    }

    private String getDecentAnnotationType(AnnotationTag aTag) {
        String type = aTag.getType();
        return type == null ? "null" : type.replace('/', '.').substring(1, type.length() - 1);
    }
}
