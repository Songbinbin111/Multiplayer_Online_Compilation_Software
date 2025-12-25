import DOMPurify from 'dompurify';

/**
 * 安全工具类
 * 提供XSS防护、输入验证等安全功能
 */
export class SecurityUtil {
  /**
   * 净化HTML内容，防止XSS攻击
   * @param html 需要净化的HTML内容
   * @returns 净化后的HTML内容
   */
  static sanitizeHtml(html: string): string {
    if (!html) return '';
    return DOMPurify.sanitize(html);
  }

  /**
   * 净化用户输入文本
   * @param text 用户输入的文本
   * @returns 净化后的文本
   */
  static sanitizeText(text: string): string {
    if (!text) return '';
    // 转义HTML特殊字符
    return text
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  /**
   * 验证URL格式
   * @param url 需要验证的URL
   * @returns 是否是有效的URL格式
   */
  static isValidUrl(url: string): boolean {
    try {
      new URL(url);
      return true;
    } catch (error) {
      return false;
    }
  }

  /**
   * 验证邮箱格式
   * @param email 需要验证的邮箱
   * @returns 是否是有效的邮箱格式
   */
  static isValidEmail(email: string): boolean {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
  }

  /**
   * 生成安全的随机字符串
   * @param length 字符串长度
   * @returns 随机字符串
   */
  static generateRandomString(length: number = 16): string {
    const charset = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    let result = '';
    const array = new Uint8Array(length);
    window.crypto.getRandomValues(array);
    for (let i = 0; i < length; i++) {
      result += charset[array[i] % charset.length];
    }
    return result;
  }
}
