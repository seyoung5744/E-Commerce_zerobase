package com.zerobase.cms.user.config.filter;

import com.zerobase.cms.user.service.CustomerService;
import com.zerobase.domain.config.JwtAuthenticationProvider;
import com.zerobase.domain.domain.common.UserVo;
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@WebFilter(urlPatterns = "/customer/*") // customer로 들어가는 모든 요청에 대한 필터
@RequiredArgsConstructor
public class CustomerFilter implements Filter {
    // 토큰은 http 프로토콜에서 header에 포함됨. 이때 어떤 key를 기준으로 토큰을 주고 받을지에 대한 key값
    public static final String TOKEN_HEADER = "X-AUTH-TOKEN";

    private final JwtAuthenticationProvider jwtAuthenticationProvider;
    private final CustomerService customerService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        String token = req.getHeader(TOKEN_HEADER);

        if(!jwtAuthenticationProvider.isValidateToken(token)){
            throw new ServletException("Invalid Access");
        }

        UserVo vo = jwtAuthenticationProvider.getUserVo(token);
        customerService.findByIdAndEmail(vo.getId(), vo.getEmail())
            .orElseThrow(() -> new ServletException("Invalid Access"));

        chain.doFilter(request, response);
    }
}
