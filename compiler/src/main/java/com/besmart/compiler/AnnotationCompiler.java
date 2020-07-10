package com.besmart.compiler;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

/**
 * 注解处理器去代码中找到专门用来标记Activity的注解
 *
 * @SupportedAnnotationTypes({"com.besmart.annotation.BindPath"})
 */
@AutoService(Processor.class)
public class AnnotationCompiler extends AbstractProcessor {

    Filer filer; // 生成java文件的工具

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        filer = processingEnvironment.getFiler();
    }

    /**
     * 声明支持的java版本
     *
     * @return
     */
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return processingEnv.getSourceVersion();
    }

    /**
     * 声明注解处理器要找的注解
     *
     * @return
     */
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new HashSet<>();
        types.add(BindPath.class.getCanonicalName());
        return types;
    }

    /**
     * 去找程序中标记的内容 都在这个方法中
     *
     * @param set
     * @param roundEnvironment
     * @return
     */
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        Set<? extends Element> elementsAnnotatedWith =
                roundEnvironment.getElementsAnnotatedWith(BindPath.class);

        Map<String, String> map = new HashMap<>();
        for (Element e : elementsAnnotatedWith) {
            TypeElement element = (TypeElement) e;
            String key = element.getAnnotation(BindPath.class).value();
            String activityName = element.getQualifiedName().toString();
            map.put(key, activityName + ".class");
        }
        // 生成文件
        if (!map.isEmpty()) {
            createClass(map);
        }

        return false;
    }

    private void createClass(Map<String, String> map) {
        try {
            MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("putActivity")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(void.class);

            Iterator<String> iterator = map.keySet().iterator();
            while (iterator.hasNext()) {
                String key = iterator.next();
                String activityName = map.get(key);
                methodBuilder.addStatement("com.besmart.router.ARouter.getInstance().addActivity(\"" + key + "\"," + activityName + ")");
            }
            MethodSpec methodSpec = methodBuilder.build();

            ClassName iRouter = ClassName.get("com.besmart.router", "IRouter");

            TypeSpec typeSpec = TypeSpec.classBuilder("ActivityUtil" + System.currentTimeMillis())
                    .addModifiers(Modifier.PUBLIC)
                    .addSuperinterface(iRouter)
                    .addMethod(methodSpec).build();

            JavaFile javaFile = JavaFile.builder("com.besmart.util", typeSpec).build();
            javaFile.writeTo(filer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}