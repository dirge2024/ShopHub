package com.shophub.controller;


import com.shophub.dto.Result;
import com.shophub.entity.ShopType;
import com.shophub.service.IShopTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/shop-type")
public class ShopTypeController {
//    @Resource
//    private IShopTypeService typeService;
   @Resource
   private IShopTypeService iShopTypeService;

    @GetMapping("list")
    public Result queryTypeList() {
//        List<ShopType> typeList = typeService
//                .query().orderByAsc("sort").list();
        return iShopTypeService.queryTypeList();
//        return Result.ok(typeList);
    }
}

