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

    @Tool("根据商家名称查询商家信息")
    public Shop findShop(@P("商家名称") String shopName) {
        return shopService.findShop(shopName);
    }
}
