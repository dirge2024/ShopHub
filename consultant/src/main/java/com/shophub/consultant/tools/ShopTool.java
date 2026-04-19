package com.shophub.consultant.tools;

import com.shophub.consultant.pojo.Shop;
import com.shophub.consultant.service.ShopService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ShopTool {

    @Autowired
    private ShopService shopService;

    //1.宸ュ叿鏂规硶: 鏌ヨ鍟嗗淇℃伅
    @Tool("鏍规嵁鍟嗗鍚嶇О鏌ヨ鍟嗗淇℃伅")
    public Shop findShop(@P("鍟嗗鍚嶇О") String shopName) {
        return shopService.findShop(shopName);
    }

}
