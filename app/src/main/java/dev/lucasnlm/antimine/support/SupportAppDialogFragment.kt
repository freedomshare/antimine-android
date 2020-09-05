package dev.lucasnlm.antimine.support

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.lifecycle.lifecycleScope
import dev.lucasnlm.antimine.R
import dev.lucasnlm.antimine.core.analytics.IAnalyticsManager
import dev.lucasnlm.antimine.core.analytics.models.Analytics
import dev.lucasnlm.antimine.core.themes.repository.IThemeRepository
import dev.lucasnlm.external.Ads
import dev.lucasnlm.external.IAdsManager
import dev.lucasnlm.external.IBillingManager
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class SupportAppDialogFragment : AppCompatDialogFragment() {
    private val billingManager: IBillingManager by inject()
    private val themeRepository: IThemeRepository by inject()
    private val analyticsManager: IAnalyticsManager by inject()
    private val adsManager: IAdsManager by inject()
    private val iapHandler: IapHandler by inject()

    private var unlockMessage: Int = R.string.support_action
    private var targetThemeId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        billingManager.start(iapHandler)
        analyticsManager.sentEvent(Analytics.ShowIapDialog)

        unlockMessage =
            (arguments?.getInt(UNLOCK_LABEL) ?: savedInstanceState?.getInt(UNLOCK_LABEL)) ?: R.string.support_action
        targetThemeId =
            (arguments?.getLong(TARGET_THEME_ID, -1L)) ?: -1L
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext()).apply {
            setView(R.layout.dialog_payments)

            if (targetThemeId != -1L) {
                setNeutralButton(R.string.try_it) { _, _ ->
                    analyticsManager.sentEvent(Analytics.UnlockRewardedDialog)
                    adsManager.requestRewarded(
                        requireActivity(),
                        Ads.RewardsAds,
                        onRewarded = {
                            if (targetThemeId > 0) {
                                themeRepository.setTheme(targetThemeId)
                                requireActivity().recreate()
                            }
                        },
                        onFail = {
                            Toast.makeText(context, R.string.sign_in_failed, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            } else {
                setNeutralButton(R.string.rating_button_no) { _, _ ->
                    analyticsManager.sentEvent(Analytics.DenyIapDialog)
                }
            }

            setPositiveButton(unlockMessage) { _, _ ->
                lifecycleScope.launch {
                    analyticsManager.sentEvent(Analytics.UnlockIapDialog)
                    billingManager.charge(requireActivity())
                }
            }
        }.create()
    }

    override fun onDismiss(dialog: DialogInterface) {
        if (activity is DialogInterface.OnDismissListener) {
            (activity as DialogInterface.OnDismissListener).onDismiss(dialog)
        }
        super.onDismiss(dialog)
    }

    companion object {
        val TAG = SupportAppDialogFragment::class.simpleName

        private const val UNLOCK_LABEL = "support_unlock_label"
        private const val TARGET_THEME_ID = "target_theme_id"

        fun newRequestSupportDialog(): SupportAppDialogFragment {
            return SupportAppDialogFragment().apply {
                arguments = Bundle().apply {
                    putInt(UNLOCK_LABEL, R.string.support_action)
                }
            }
        }

        fun newRemoveAdsSupportDialog(): SupportAppDialogFragment {
            return SupportAppDialogFragment().apply {
                arguments = Bundle().apply {
                    putInt(UNLOCK_LABEL, R.string.remove_ad)
                }
            }
        }

        fun newChangeThemeDialog(targetThemeId: Long): SupportAppDialogFragment {
            return SupportAppDialogFragment().apply {
                arguments = Bundle().apply {
                    putInt(UNLOCK_LABEL, R.string.unlock)
                    putLong(TARGET_THEME_ID, targetThemeId)
                }
            }
        }
    }
}
