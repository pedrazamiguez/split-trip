package es.pedrazamiguez.splittrip.features.expense.presentation.extensions

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.BuildingBank
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.CashBanknote
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.CreditCard
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Qrcode
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Wallet
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.features.expense.R

@StringRes
fun PaymentMethod.toStringRes(): Int = when (this) {
    PaymentMethod.CASH -> R.string.payment_method_cash
    PaymentMethod.BIZUM -> R.string.payment_method_bizum
    PaymentMethod.PIX -> R.string.payment_method_pix
    PaymentMethod.CREDIT_CARD -> R.string.payment_method_credit_card
    PaymentMethod.DEBIT_CARD -> R.string.payment_method_debit_card
    PaymentMethod.BANK_TRANSFER -> R.string.payment_method_bank_transfer
    PaymentMethod.PAYPAL -> R.string.payment_method_paypal
    PaymentMethod.VENMO -> R.string.payment_method_venmo
    PaymentMethod.ALIPAY -> R.string.payment_method_alipay
    PaymentMethod.WECHAT_PAY -> R.string.payment_method_wechat_pay
    PaymentMethod.OTHER -> R.string.payment_method_other
}

fun PaymentMethod.toIconVector(): ImageVector = when (this) {
    PaymentMethod.CASH -> TablerIcons.Outline.CashBanknote
    PaymentMethod.BIZUM -> TablerIcons.Outline.Qrcode
    PaymentMethod.PIX -> TablerIcons.Outline.Qrcode
    PaymentMethod.CREDIT_CARD -> TablerIcons.Outline.CreditCard
    PaymentMethod.DEBIT_CARD -> TablerIcons.Outline.CreditCard
    PaymentMethod.BANK_TRANSFER -> TablerIcons.Outline.BuildingBank
    PaymentMethod.PAYPAL -> TablerIcons.Outline.Wallet
    PaymentMethod.VENMO -> TablerIcons.Outline.Wallet
    PaymentMethod.ALIPAY -> TablerIcons.Outline.Qrcode
    PaymentMethod.WECHAT_PAY -> TablerIcons.Outline.Qrcode
    PaymentMethod.OTHER -> TablerIcons.Outline.Wallet
}
