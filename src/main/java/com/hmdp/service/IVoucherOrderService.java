package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.Result;
import com.hmdp.entity.VoucherOrder;

public interface IVoucherOrderService extends IService<VoucherOrder> {
    Result seckillVoucher(Long voucherId);

//    public  Result createVoucherOrder(Long voucherId);
    void createVoucherOrder(VoucherOrder voucherOrder);

}
