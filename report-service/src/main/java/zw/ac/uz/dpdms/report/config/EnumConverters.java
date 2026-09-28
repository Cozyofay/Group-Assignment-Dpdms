package zw.ac.uz.dpdms.report.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import zw.ac.uz.dpdms.report.dto.ReportFormat;

import java.util.Locale;

/** Lets the URL use lower case (/api/reports/pdf) while the enum stays upper case. */
@Configuration
public class EnumConverters implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new Converter<String, ReportFormat>() {
            @Override
            public ReportFormat convert(String source) {
                return ReportFormat.valueOf(source.trim().toUpperCase(Locale.ROOT));
            }
        });
    }
}
