package com.mycompany.bebaAgua;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.file.Path;
import static java.lang.foreign.ValueLayout.*;

/** Consulta somente leitura da preferência de animações do Windows (SPI_GETCLIENTAREAANIMATION). */
final class MovimentoWindows {
    private static final MethodHandle consultar=carregar();
    private static MethodHandle carregar(){
        if(!System.getProperty("os.name","").startsWith("Windows")||!MovimentoWindows.class.getModule().isNativeAccessEnabled())return null;
        try{
            var lib=SymbolLookup.libraryLookup(Path.of(System.getenv("SystemRoot"),"System32","user32.dll"),Arena.global());
            return Linker.nativeLinker().downcallHandle(lib.find("SystemParametersInfoW").orElseThrow(),FunctionDescriptor.of(JAVA_INT,JAVA_INT,JAVA_INT,ADDRESS,JAVA_INT));
        }catch(RuntimeException|LinkageError e){return null;}
    }
    static boolean suportado(){return consultar!=null;}
    static boolean animacoesPermitidas(){
        if(consultar==null)return true; // Controle Efeitos permanece disponível em outros sistemas.
        try(Arena arena=Arena.ofConfined()){
            MemorySegment valor=arena.allocate(JAVA_INT);int ok=(int)consultar.invokeExact(0x1042,0,valor,0);
            return ok==0||valor.get(JAVA_INT,0)!=0;
        }catch(Throwable e){return true;}
    }
}
