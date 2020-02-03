package com.ht.weichat.controller.web;

import com.ht.weichat.pojo.TbAccount;
import com.ht.weichat.pojo.TbArticle;
import com.ht.weichat.pojo.TbType;
import com.ht.weichat.service.ArticleService;
import com.ht.weichat.service.SalesService;
import com.ht.weichat.service.TypeService;
import com.ht.weichat.utils.ConstantPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author cz
 */
@Controller
public class PageController {

    @Autowired
    private TypeService typeService;

    @Autowired
    private ArticleService articleService;

    @Autowired
    private SalesService salesService;

    @RequestMapping("/")
    public String showIndex(Model model) {
        return "login";
    }


    @RequestMapping("/query_yesterday_sales")
    public String showYesterdaySales(Model model) {
        String result=salesService.yesterday();
        if(result.equals("access_token已过期")){
            model.addAttribute("codeUrl", salesService.getCodeUrl());

        }
        model.addAttribute("yesterday_sales", result);

        return "query_yesterday_sales";
    }
    @RequestMapping("/get_access_token")
    public String refreshAccessToken(Model model,HttpServletRequest request) {
        String code = request.getParameter("code");
        String token=salesService.getAccessTokenFromCode(code);
        model.addAttribute("access_token", token);
        String result=salesService.yesterday();
        model.addAttribute("yesterday_sales", result);
        return "query_yesterday_sales";
    }

    @RequestMapping("/query_unsend_sales")
    public String showUnSendSales(Model model) {
        String result=salesService.unsend();
        model.addAttribute("unsend_sales", result);

        return "query_unsend_sales";
    }
    @RequestMapping("/sync_sales")
    public String showSyncSales(Model model) {
        String result=salesService.sync();
        model.addAttribute("sales", result);

        return "sync_sales";
    }

    @RequestMapping("/admin")
    public String showAdmin(Model model, HttpServletRequest request) {
        TbAccount account = (TbAccount) request.getSession().getAttribute("global.account");
//        if (account == null){
            return "login";
//        } else {
//            return "redirect:/home";
//        }
    }


    @RequestMapping("/home")
    public String home(
        Model model,
        @RequestParam(defaultValue = "0") int id
    ) {

        List<TbType> titleList = typeService.list();

        if (titleList.size() == 0){
            return "redirect:/type/home";
        }

        model.addAttribute("titleList", titleList);

        TbType defaultTitle = null;
        if (id == 0){
            defaultTitle = titleList.get(0);
            model.addAttribute("topTitle", defaultTitle);
        } else {
            defaultTitle = typeService.findById(id);
            model.addAttribute("topTitle", defaultTitle);
        }

        List<TbArticle> countList = articleService.listById(defaultTitle.getId());
        int pageSize = countList.size()/ConstantPool.PAGESIZE;
        model.addAttribute("totalCount", countList.size() % ConstantPool.PAGESIZE==0?pageSize:pageSize+1);

        List<TbArticle> itemList = articleService.listById(defaultTitle.getId(), 1);

        for (int i = 0; i < itemList.size(); i++) {
            TbArticle tbArticle = itemList.get(i);
            if (tbArticle.getContent() != null && tbArticle.getContent().length() > 50){
                tbArticle.setContent(tbArticle.getContent().substring(0, 50));
            }
        }

        model.addAttribute("itemList", itemList);
        return "home";
    }

}
