package com.plumsnow.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.plumsnow.context.BaseContext;
import com.plumsnow.entity.OrderDetail;
import com.plumsnow.entity.Orders;
import com.plumsnow.entity.User;
import com.plumsnow.result.Result;
import com.plumsnow.service.OrdersDetailService;
import com.plumsnow.service.OrdersService;
import com.plumsnow.service.UserService;
import com.plumsnow.service.WorkspaceService;
import com.plumsnow.vo.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@RestController
@RequestMapping("admin/report")
@Api(tags = "数据统计相关接口")
@Slf4j
public class ReportController {
    @Autowired
    private OrdersService ordersService;
    @Autowired
    private UserService userService;
    @Autowired
    private OrdersDetailService ordersDetailService;
    @Autowired
    private WorkspaceService workspaceService;

    /**
     * 营业额统计
     * @param begin
     * @param end
     * @return
     */
    @GetMapping("/turnoverStatistics")
    @ApiOperation("营业额统计")
    public Result<TurnoverReportVO> turnoverStatistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd")  LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end){

        List<LocalDate> dataList = new ArrayList<>();

        LocalDate l = begin;
        while(!l.isEqual(end)){
            l = l.plusDays(1);
            dataList.add(l);
        }

        String join = StringUtils.join(dataList, ",");

        LambdaQueryWrapper<Orders> lambdaQueryWrapper  = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.between(Orders::getOrderTime, begin, end);
        lambdaQueryWrapper.eq(Orders::getShopId, BaseContext.getCurrentShopId());
        List<Orders> ordersList = ordersService.list(lambdaQueryWrapper);

        List<BigDecimal> turnoverList = new ArrayList<>();
        for(LocalDate date : dataList){
            LocalDateTime localDateTimeMax = LocalDateTime.of(date, LocalTime.MAX);
            LocalDateTime localDateTimeMin = LocalDateTime.of(date, LocalTime.MIN);

            BigDecimal turnover = new BigDecimal(0);

            for(Orders orders : ordersList){
                if (Objects.equals(orders.getStatus(), Orders.DELIVERY_IN_PROGRESS)) {
                    if (orders.getOrderTime().isAfter(localDateTimeMin) && orders.getOrderTime().isBefore(localDateTimeMax)) {
                        turnover = turnover.add(orders.getAmount());
                    }
                }
            }

            turnoverList.add(turnover);
        }

        String turnoverJoin = StringUtils.join(turnoverList, ",");

        return Result.success(new TurnoverReportVO(join, turnoverJoin));
    }

    @GetMapping("/ordersStatistics")
    @ApiOperation("订单统计")
    public Result<OrderReportVO> ordersStatistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd")  LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end){

        List<LocalDate> dataList = new ArrayList<>();

        LocalDate l = begin;
        while(!l.isEqual(end)){
            l = l.plusDays(1);
            dataList.add(l);
        }

        String dateJoin = StringUtils.join(dataList, ",");

        LambdaQueryWrapper<Orders> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.between(Orders::getOrderTime, begin, end);
        lambdaQueryWrapper.eq(Orders::getShopId, BaseContext.getCurrentShopId());
        List<Orders> ordersList = ordersService.list(lambdaQueryWrapper);

        List<Integer> orderTotalCountList = new ArrayList<>();
        List<Integer> orderEffectiveCountList = new ArrayList<>();

        for(LocalDate date : dataList){
            Integer orderTotalCount = 0;
            Integer orderEffectiveCount = 0;

            for(Orders orders : ordersList){
                if(orders.getOrderTime().isAfter(LocalDateTime.of(date, LocalTime.MIN)) && orders.getOrderTime().isBefore(LocalDateTime.of(date, LocalTime.MAX))) {
                    if (Objects.equals(orders.getStatus(), Orders.DELIVERY_IN_PROGRESS)) {
                        orderEffectiveCount++;
                    }
                    orderTotalCount++;
                }
            }
            
            orderTotalCountList.add(orderTotalCount);
            orderEffectiveCountList.add(orderEffectiveCount);
        }

        String orderCountList = StringUtils.join(orderTotalCountList, ",");
        String validOrderCountList = StringUtils.join(orderEffectiveCountList, ",");

        Integer orderTotalCount = 0;
        for(Integer i : orderTotalCountList){
            orderTotalCount += i;
        }

        Integer orderEffectiveCount = 0;
        for(Integer i : orderEffectiveCountList){
            orderEffectiveCount += i;
        }

        // 订单完成率
        Double orderCompletionRate = orderEffectiveCount * 1.0 / orderTotalCount;

        OrderReportVO orderReportVO = new OrderReportVO(
                dateJoin,
                orderCountList,
                validOrderCountList,
                orderTotalCount,
                orderEffectiveCount,
                orderCompletionRate);

        return Result.success(orderReportVO);
    }
    
    @GetMapping("/top10")
    @ApiOperation("销售排行")
    public Result<SalesTop10ReportVO> salesTop10Statistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd")  LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end){

        List<LocalDate> dataList = new ArrayList<>();

        LocalDate l = begin;
        while(!l.isEqual(end)){
            l = l.plusDays(1);
            dataList.add(l);
        }

        String dateJoin = StringUtils.join(dataList, ",");

        LambdaQueryWrapper<Orders> lambdaQueryWrapper4Orders = new LambdaQueryWrapper<>();
        lambdaQueryWrapper4Orders.between(Orders::getOrderTime, begin, end);
        lambdaQueryWrapper4Orders.eq(Orders::getShopId, BaseContext.getCurrentShopId());
        List<Orders> ordersList = ordersService.list(lambdaQueryWrapper4Orders);

        Map<String, Integer> map = new HashMap<>();

        for(Orders orders : ordersList){
            if(Objects.equals(orders.getStatus(), Orders.DELIVERY_IN_PROGRESS)){
                LambdaQueryWrapper<OrderDetail> lamdaQueryWrapper4OrderDetail = new LambdaQueryWrapper<>();
                lamdaQueryWrapper4OrderDetail.eq(OrderDetail::getShopId, BaseContext.getCurrentShopId());
                lamdaQueryWrapper4OrderDetail.eq(OrderDetail::getOrderId, orders.getId());
                List<OrderDetail> orderDetailList = ordersDetailService.list(lamdaQueryWrapper4OrderDetail);

                for(OrderDetail orderDetail : orderDetailList){
                    map.put(orderDetail.getName(), orderDetail.getNumber());
                }

            }

        }

        //对map根据数量进行降序排序，仅保留前十项
        List<String> top10 = new ArrayList<>();
        List<Integer> top10Count = new ArrayList<>();
        String maxKey = "";
        Integer max = 0;
        while(!map.isEmpty() && top10.size() < 10) {

            for (Map.Entry<String, Integer> entry : map.entrySet()) {

                if (entry.getValue() > max) {
                    max = entry.getValue();
                    maxKey = entry.getKey();
                }

            }

            top10.add(maxKey);
            top10Count.add(max);
            map.remove(maxKey);
            max = 0;
            maxKey = "";
        }

        String top10Join = StringUtils.join(top10, ",");
        String top10CountJoin = StringUtils.join(top10Count, ",");


        return Result.success(new SalesTop10ReportVO(top10Join, top10CountJoin));
    }

    @GetMapping("/export")
    @ApiOperation("导出运营数据报表")
    public void export(HttpServletResponse httpServletResponse){
        //1. 查询数据库，获取营业数据---查询最近30天的运营数据
        //往前倒30天
        LocalDate begin = LocalDate.now().minusDays(30);
        //往前倒一天
        LocalDate end = LocalDate.now().minusDays(1);

        //查询概览数据
        BusinessDataVO businessDataVO = workspaceService.getBusinessData(
                LocalDateTime.of(begin, LocalTime.MIN),
                LocalDateTime.of(end, LocalTime.MAX));

        //2. 封装数据进Excel
        //获取项目的类加载器，然后使用类加载器从项目的资源文件中读取一个 Excel 模板文件
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/Template.xlsx");
        try {
            //基于模板文件创建一个新的Excel文件
            XSSFWorkbook excel = new XSSFWorkbook(in);

            //获取模板中的sheet
            XSSFSheet sheet1 = excel.getSheet("sheet1");

            //填充数据--时间
            sheet1.getRow(1).getCell(1).setCellValue("时间：" + begin + "至" + end);

            //获取第四行
            XSSFRow row = sheet1.getRow(3);
            row.getCell(2).setCellValue(businessDataVO.getTurnover());
            row.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
            //row.getCell(6).setCellValue(businessDataVO.getNewUsers());

            //获得第5行
            row = sheet1.getRow(4);
            row.getCell(2).setCellValue(businessDataVO.getValidOrderCount());
            row.getCell(4).setCellValue(businessDataVO.getUnitPrice());

            //填充明细数据
            for (int i = 0; i < 30; i++) {
                LocalDate date = begin.plusDays(i);
                //查询某一天的营业数据
                BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));

                //获得某一行
                row = sheet1.getRow(7 + i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessData.getTurnover());
                row.getCell(3).setCellValue(businessData.getValidOrderCount());
                row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessData.getUnitPrice());
                //row.getCell(6).setCellValue(businessData.getNewUsers());
            }

            httpServletResponse.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            httpServletResponse.setHeader("Content-Disposition", "attachment; filename=OperationalDataReport.xlsx");

            //3.通过输出流将Excel下载到前端
            ServletOutputStream out = httpServletResponse.getOutputStream();
            excel.write(out);

            //关闭资源
            out.close();
            excel.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}

