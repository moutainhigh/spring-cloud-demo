package cn.buildworld.elasticjob.job.goods;

import com.alibaba.dubbo.config.annotation.Reference;
import com.dangdang.ddframe.job.api.ShardingContext;
import com.dangdang.ddframe.job.api.simple.SimpleJob;
import com.dangdang.elasticjob.lite.annotation.ElasticSimpleJob;
import com.iwhalecloud.retail.dto.ResultVO;
import com.iwhalecloud.retail.goods2b.common.GoodsConst;
import com.iwhalecloud.retail.goods2b.service.dubbo.GoodsSaleNumService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Created by Administrator on 2019/6/22.
 */
@ElasticSimpleJob(cron = "0 1 1 * * ?",
        jobName = "ProductSaleOrderJob",
        shardingTotalCount = 1,
        jobParameter = "测试参数",
        shardingItemParameters = "0=A,1=B",
        dataSource = "datasource")
@Component
@Slf4j
public class ProductSaleOrderJob implements SimpleJob {
    @Reference(timeout = 60000)
    private GoodsSaleNumService goodsSaleNumService;

    @Override
    public void execute(ShardingContext shardingContext) {
        Date startDate = new Date();
        log.info("ProductSaleOrderJob run start.....{}", startDate);
        try {
            //先清缓存再写入
            ResultVO<Boolean> resultVO = goodsSaleNumService.cleanCacheProductSaleNum();
            if(resultVO.isSuccess() && resultVO.getResultData()){
                goodsSaleNumService.getProductSaleOrder(GoodsConst.CACHE_NAME_PRODUCT_SALE_ORDER_WHOLE);
            }

        }catch (Exception ex) {
            log.error("ProductSaleOrderJob getProductSaleOrder throw exception ex={}", ex);
        }

        Date endDate = new Date();
        log.info("ProductSaleOrderJob run end.....{}", endDate);
    }
}
