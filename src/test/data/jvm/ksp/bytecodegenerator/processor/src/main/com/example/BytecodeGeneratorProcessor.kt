package com.example

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import src.test.data.jvm.ksp.bytecodegenerator.annotation.GenerateBytecode

/**
 * An example KSP processor that generates bytecode to be used only for tests
 */
class BytecodeGeneratorProcessor(
    private val codeGen: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {
  override fun process(resolver: Resolver): List<KSAnnotated> {
    val annotated = resolver
      .getSymbolsWithAnnotation(GenerateBytecode::class.qualifiedName!!)
      .filterIsInstance<KSClassDeclaration>()
      .toList()
    if (annotated.isEmpty()) return emptyList()

    annotated.forEach { cls ->
      val origName = cls.simpleName.asString()
      val genName  = "${origName}\$GeneratedDefinition\$"
      val pkg       = cls.packageName.asString()
      val internal = pkg.replace('.', '/') + "/" + genName

      // Build class with ASM
      val cw = ClassWriter(ClassWriter.COMPUTE_FRAMES or ClassWriter.COMPUTE_MAXS)
      cw.visit(
          Opcodes.V1_8,
        Opcodes.ACC_PUBLIC or Opcodes.ACC_SUPER,
        internal,
        null,
        "java/lang/Object",
        null
      )

      // Constructor
      cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null).apply {
        visitCode()
        visitVarInsn(Opcodes.ALOAD, 0)
        visitMethodInsn(
            Opcodes.INVOKESPECIAL,
          "java/lang/Object", "<init>", "()V", false
        )
        visitInsn(Opcodes.RETURN)
        visitMaxs(0, 0)
        visitEnd()
      }

      // greet() method
      cw.visitMethod(
          Opcodes.ACC_PUBLIC,
        "greet",
        "()Ljava/lang/String;",
        null,
        null
      ).apply {
        visitCode()
        visitLdcInsn("Hello, $origName!")
        visitInsn(Opcodes.ARETURN)
        visitMaxs(0, 0)
        visitEnd()
      }

      cw.visitEnd()
      val bytecode = cw.toByteArray()

      // Write out .class file
      val deps = Dependencies(false, *annotated.mapNotNull { it.containingFile }.toTypedArray())
      val out = codeGen.createNewFile(dependencies = deps, packageName = pkg, fileName = genName, extensionName =  "class")
      out.use { it.write(bytecode) }

      // Generate service file
      val fqcn = "$pkg.$genName"
      val serviceFile = codeGen.createNewFile(
        dependencies = Dependencies(false),
        packageName = "META-INF.services",
        fileName = fqcn,
        extensionName = ""
      )
      serviceFile.bufferedWriter().use { it.appendLine(fqcn) }

      logger.info("Generated $pkg.$genName")
    }

    return emptyList()
  }
}
