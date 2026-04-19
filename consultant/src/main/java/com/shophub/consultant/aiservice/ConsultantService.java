package com.shophub.consultant.aiservice;

import com.shophub.consultant.tools.ReservationTool;
import com.shophub.consultant.tools.ShopTool;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;
import reactor.core.publisher.Flux;

@AiService(
        wiringMode = AiServiceWiringMode.EXPLICIT,//鎵嬪姩瑁呴厤
        chatModel = "openAiChatModel",//鎸囧畾妯″瀷
        streamingChatModel = "openAiStreamingChatModel",
        //chatMemory = "chatMemory",//閰嶇疆浼氳瘽璁板繂瀵硅薄
        chatMemoryProvider = "chatMemoryProvider",//閰嶇疆浼氳瘽璁板繂鎻愪緵鑰呭璞?
        contentRetriever = "contentRetriever",//閰嶇疆鍚戦噺鏁版嵁搴撴绱㈠璞?
        tools = {"shopTool","reservationTool","voucherTool"}
)
//@AiService
public interface ConsultantService {
    
    @SystemMessage(fromResource = "system.txt")
     public Flux<String> chat(/*@V("msg")*/@MemoryId String memoryId, @UserMessage String message);
    //鐢ㄤ簬鑱婂ぉ鐨勬柟娉?
    //public String chat(String message);
    //@SystemMessage("浣犳槸涓滃摜鐨勫姪鎵嬪皬鏈堟湀,浜虹編蹇冨杽鍙堝閲?")
    //@UserMessage("浣犳槸涓滃摜鐨勫姪鎵嬪皬鏈堟湀,浜虹編蹇冨杽鍙堝閲?{{it}}")
    //@UserMessage("浣犳槸涓滃摜鐨勫姪鎵嬪皬鏈堟湀,浜虹編蹇冨杽鍙堝閲?{{msg}}")
   
}
