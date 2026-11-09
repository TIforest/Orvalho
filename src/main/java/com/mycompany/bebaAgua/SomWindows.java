package com.mycompany.bebaAgua;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.file.Path;
import static java.lang.foreign.ValueLayout.*;

/** Reproduz o WAV pela saída padrão do Windows, sem extrair arquivos temporários. */
final class SomWindows {
    private static final int SND_MEMORY=0x0004, SND_NODEFAULT=0x0002;
    private static final MethodHandle tocar=carregar();

    private static MethodHandle carregar(){
        if(!System.getProperty("os.name","").startsWith("Windows")||!SomWindows.class.getModule().isNativeAccessEnabled())return null;
        try{
            var dll=SymbolLookup.libraryLookup(Path.of(System.getenv("SystemRoot"),"System32","winmm.dll"),Arena.global());
            return Linker.nativeLinker().downcallHandle(dll.find("PlaySoundW").orElseThrow(),
                    FunctionDescriptor.of(JAVA_INT,ADDRESS,ADDRESS,JAVA_INT));
        }catch(RuntimeException|LinkageError e){return null;}
    }

    static boolean reproduzir(byte[] wav){
        if(tocar==null||wav.length==0)return false;
        // PlaySound síncrono: a memória permanece válida até o fim do toque.
        try(Arena arena=Arena.ofConfined()){
            var memoria=arena.allocateFrom(JAVA_BYTE,wav);
            return (int)tocar.invokeExact(memoria,MemorySegment.NULL,SND_MEMORY|SND_NODEFAULT)!=0;
        }catch(Throwable erro){
            if(erro instanceof VirtualMachineError fatal)throw fatal;
            if(erro instanceof ThreadDeath fatal)throw fatal;
            System.getLogger(SomWindows.class.getName()).log(System.Logger.Level.DEBUG,"Saída nativa indisponível",erro);
            return false;
        }
    }
}
