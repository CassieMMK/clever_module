package com.avalon.jpcap.common.filter;

import com.alibaba.fastjson.JSONObject;
import com.avalon.jpcap.common.filter.spi.LoginContextPostProcessor;
import com.avalon.jpcap.common.filter.vo.LoginVO;
import com.avalon.jpcap.common.utils.SpringApplicationContextUtil;
import com.google.common.collect.Lists;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * 线程安全的登录态上下文context的获取工具类
 * @author SoulW
 * @since 2021/5/19 18:15
 */
public class LoginContextHelper {

    /**
     * 自定义扩展
     */
    public static final Collection<LoginContextPostProcessor> SPIS;
    private static final String JNOS_U_ID = "jnos-user-uid";
    private static final String JNOS_ACCOUNT_ID = "jnos-user-accountid";

    static {
        //1、使用META-INF factories文件加载spi方式，相对来说麻烦一些，优势是使得第三方服务模块的装配控制的逻辑与调用者的业务代码分离，只需要选择性启用META-INF中的配置文件就行，而不是@Component注册耦合在一起。
        // 应用程序可以根据实际业务情况启用框架扩展或替换框架组件。
//        List<LoginContextPostProcessor> spis = Lists.newArrayList();
//        ServiceLoader<LoginContextPostProcessor> serviceLoader = ServiceLoader.load(LoginContextPostProcessor.class);
//        serviceLoader.iterator().forEachRemaining(spis::add);
//        SPIS = Collections.unmodifiableCollection(spis);

        //2、使用spring IOC 静态方法注入方式将bean类注入到非bean类中，更简单, LoginContextPostProcessor的实现类们用@Component注册为bean即可
        List<LoginContextPostProcessor> spis = Lists.newArrayList();
        spis.addAll(SpringApplicationContextUtil.getBeansOfType(LoginContextPostProcessor.class).values());
        SPIS = Collections.unmodifiableCollection(spis);
    }

    /**
     * 加载登录信息，如果没有SPI的实现类，则默认尝试去获取LoginVO中的Pin、VenderId、identity
     * 如果需要加载别的header头，则在SPI实现类中写，去覆盖result的值
     * 如果需要解析cookie，需要在前面写好过滤器，从cookie中解析出值，并把解析出的值放进header头中，以便在这里可以获取
     *
     * @return 登录信息
     */
    public static LoginVO loadLoginInfo() {
        //RequestContextHolder: Holder类，以线程绑定的RequestAttributes对象的形式公开web请求
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        if (!(requestAttributes instanceof ServletRequestAttributes)) {
            return new LoginVO();
        }

        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        String pin = request.getHeader(LoginVO.HEADER_PIN);
        String venderId = request.getHeader(LoginVO.HEADER_VENDER_ID);
        String identity = request.getHeader(LoginVO.HEADER_IDENTITY);

        LoginVO result = new LoginVO();
        result.setPin(pin);
        result.setVenderId(venderId);
        result.setIdentity(identity);

        CollectionUtils.emptyIfNull(SPIS).stream()
                .filter(Objects::nonNull)
                .forEach(each -> each.afterLogin(request, result));
        return result;
    }

    /**
     * 获取商家ID
     *
     * @return 商家ID
     */
    public static Long getVenderId() {
        LoginVO loginVO = loadLoginInfo();
        if (StringUtils.isNumeric(loginVO.getVenderId())) {
            return Long.valueOf(loginVO.getVenderId());
        }
        return null;
    }

    /**
     * 获取JNOS的Uid
     *
     * @return 结果
     */
    @Nullable
    public static Long getJnosUid() {
        return getExtLongField(JNOS_U_ID);
    }

    /**
     * 获取JNOS-账户ID
     *
     * @return 账户ID
     */
    @Nullable
    public static Long getJnosAccountId() {
        return getExtLongField(JNOS_ACCOUNT_ID);
    }

    /**
     * 获取BuID
     *
     * @return BU-ID
     */
    @Nullable
    public static Integer getBuId() {
        String buId = loadLoginInfo().getBuId();
        if (StringUtils.isBlank(buId)) {
            return null;
        }
        return Integer.valueOf(buId);
    }

    /**
     * 获取Long值
     *
     * @param code 代码
     * @return 结果
     */
    @Nullable
    public static Long getExtLongField(String code) {
        LoginVO loginVo = loadLoginInfo();
        Map<String, Object> ext = loginVo.getExt();
        if (Objects.nonNull(ext)) {
            return new JSONObject(ext).getLong(code);
        }
        return null;
    }

    /**
     * 获取String值
     *
     * @param code 代码
     * @return 结果
     */
    @Nullable
    public static String getExtStringField(String code) {
        LoginVO loginVO = loadLoginInfo();
        Map<String, Object> ext = loginVO.getExt();
        if (Objects.nonNull(ext)) {
            return new JSONObject(ext).getString(code);
        }
        return null;
    }
}
