package ir.tax.trustedtaxpayers.core.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * BaseDto حاوی فیلدهای مشترک بین DTO ها
 * 
 * <p>ویژگی‌ها:</p>
 * <ul>
 *   <li>ID به صورت String (encoded UUID) برای امنیت بیشتر</li>
 *   <li>اطلاعات Auditing (created/updated dates and users)</li>
 *   <li>پشتیبانی از soft delete با isActive flag</li>
 * </ul>
 */
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class BaseDto implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	/**
	 * شناسه یکتا به صورت String (encoded UUID)
	 */
	private String id;
	
	/**
	 * تاریخ ایجاد
	 */
	private OffsetDateTime createdAt;
	
	/**
	 * تاریخ آخرین به‌روزرسانی
	 */
	private OffsetDateTime updatedAt;
	
	/**
	 * کاربر ایجادکننده
	 */
	private String createdBy;
	
	/**
	 * کاربر آخرین به‌روزرسانی‌کننده
	 */
	private String updatedBy;
	
	/**
	 * وضعیت فعال بودن
	 */
	@Builder.Default
	private boolean isActive = true;
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		BaseDto baseDto = (BaseDto) o;
		return Objects.equals(id, baseDto.id);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "{" +
				"id='" + id + '\'' +
				", isActive=" + isActive +
				", createdAt=" + createdAt +
				'}';
	}
}

