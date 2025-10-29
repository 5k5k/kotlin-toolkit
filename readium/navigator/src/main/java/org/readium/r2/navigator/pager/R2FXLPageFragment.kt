/*
 * Module: r2-navigator-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann, Mostapha Idoubihi, Paul Stoica
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.navigator.pager

import android.annotation.SuppressLint
import android.app.Activity
import android.content.res.Configuration
import android.graphics.PointF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.widget.RelativeLayout
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.webkit.WebViewClientCompat
import org.readium.r2.navigator.R2BasicWebView
import org.readium.r2.navigator.databinding.ReadiumNavigatorFragmentFxllayoutDoubleBinding
import org.readium.r2.navigator.databinding.ReadiumNavigatorFragmentFxllayoutDoubleLandscapeBinding
import org.readium.r2.navigator.databinding.ReadiumNavigatorFragmentFxllayoutSingleLandscapeBinding
import org.readium.r2.navigator.databinding.ReadiumNavigatorFragmentFxllayoutSingleBinding
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubNavigatorViewModel
import org.readium.r2.navigator.epub.fxl.R2FXLLayout
import org.readium.r2.navigator.epub.fxl.R2FXLOnDoubleTapListener
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.util.Url

@Suppress("DEPRECATION")
internal class R2FXLPageFragment : Fragment() {

    private val firstResourceUrl: Url?
        get() = BundleCompat.getParcelable(requireArguments(), "firstUrl", Url::class.java)

    private val secondResourceUrl: Url?
        get() = BundleCompat.getParcelable(requireArguments(), "secondUrl", Url::class.java)

    private val firstResourceLink: Link?
        get() = BundleCompat.getParcelable(requireArguments(), "firstLink", Link::class.java)

    private val secondResourceLink: Link?
        get() = BundleCompat.getParcelable(requireArguments(), "secondLink", Link::class.java)

    private var webViews = mutableListOf<R2BasicWebView>()

    private var _doubleBinding: ReadiumNavigatorFragmentFxllayoutDoubleBinding? = null
    private val doubleBinding get() = _doubleBinding!!

    private var _singleBinding: ReadiumNavigatorFragmentFxllayoutSingleBinding? = null
    private var _singleLandscapeBinding: ReadiumNavigatorFragmentFxllayoutSingleLandscapeBinding? = null
    private val singleBinding get() = _singleBinding!!
    private val singleLandscapeBinding get() = _singleLandscapeBinding!!

    private var _doubleLandscapeBinding: ReadiumNavigatorFragmentFxllayoutDoubleLandscapeBinding? = null
    private val doubleLandscapeBinding get() = _doubleLandscapeBinding!!

    private val navigator: EpubNavigatorFragment?
        get() = parentFragment as? EpubNavigatorFragment

    private val viewModel: EpubNavigatorViewModel by viewModels(
        ownerProducer = { requireParentFragment() }
    )

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        secondResourceUrl?.let {
            if (firstResourceUrl == null ){
                val landscape = isLandscape()
                if (landscape) {
                    _singleLandscapeBinding = ReadiumNavigatorFragmentFxllayoutSingleLandscapeBinding.inflate(
                        inflater,
                        container,
                        false
                    )
                } else {
                    _singleBinding = ReadiumNavigatorFragmentFxllayoutSingleBinding.inflate(
                        inflater,
                        container,
                        false
                    )
                }

                val view: View = if (landscape) singleLandscapeBinding.root else singleBinding.root
                view.setPadding(0, 0, 0, 0)

                val r2FXLLayout = if (landscape) singleLandscapeBinding.r2FXLLayout else singleBinding.r2FXLLayout
                r2FXLLayout.isAllowParentInterceptOnScaled = true

                val webview = if (landscape) singleLandscapeBinding.webViewSingle else singleBinding.webViewSingle

                setupWebView(webview, secondResourceLink, secondResourceUrl)

                r2FXLLayout.addOnDoubleTapListener(R2FXLOnDoubleTapListener(true))
                r2FXLLayout.addOnTapListener(object : R2FXLLayout.OnTapListener {
                    override fun onTap(view: R2FXLLayout, info: R2FXLLayout.TapInfo): Boolean {
                        return webview.listener?.onTap(PointF(info.x, info.y)) ?: false
                    }
                })

                return view
            } else {
                val landscape = isLandscape()
                if (landscape) {
                    _doubleLandscapeBinding = ReadiumNavigatorFragmentFxllayoutDoubleLandscapeBinding.inflate(
                        inflater,
                        container,
                        false
                    )
                } else {
                    _doubleBinding = ReadiumNavigatorFragmentFxllayoutDoubleBinding.inflate(
                        inflater,
                        container,
                        false
                    )
                }

                val view: View = if (landscape) doubleLandscapeBinding.root else doubleBinding.root
                view.setPadding(0, 0, 0, 0)

                val r2FXLLayout = if (landscape) doubleLandscapeBinding.r2FXLLayout else doubleBinding.r2FXLLayout
                r2FXLLayout.isAllowParentInterceptOnScaled = true

                val left = if (landscape) doubleLandscapeBinding.firstWebView else doubleBinding.firstWebView
                val right = if (landscape) doubleLandscapeBinding.secondWebView else doubleBinding.secondWebView

                setupWebView(left, firstResourceLink, firstResourceUrl, true)
                setupWebView(right, secondResourceLink, secondResourceUrl, false)

                r2FXLLayout.addOnDoubleTapListener(R2FXLOnDoubleTapListener(true))
                r2FXLLayout.addOnTapListener(object : R2FXLLayout.OnTapListener {
                    override fun onTap(view: R2FXLLayout, info: R2FXLLayout.TapInfo): Boolean {
                        return left.listener?.onTap(PointF(info.x, info.y)) ?: false
                    }
                })

                return view
            }
        } ?: run {
            val landscape = isLandscape()
            if (landscape) {
                _singleLandscapeBinding = ReadiumNavigatorFragmentFxllayoutSingleLandscapeBinding.inflate(
                    inflater,
                    container,
                    false
                )
            } else {
                _singleBinding = ReadiumNavigatorFragmentFxllayoutSingleBinding.inflate(
                    inflater,
                    container,
                    false
                )
            }

            val view: View = if (landscape) singleLandscapeBinding.root else singleBinding.root
            view.setPadding(0, 0, 0, 0)

            val r2FXLLayout = if (landscape) singleLandscapeBinding.r2FXLLayout else singleBinding.r2FXLLayout
            r2FXLLayout.isAllowParentInterceptOnScaled = true

            val webview = if (landscape) singleLandscapeBinding.webViewSingle else singleBinding.webViewSingle

            setupWebView(webview, firstResourceLink, firstResourceUrl)

            r2FXLLayout.addOnDoubleTapListener(R2FXLOnDoubleTapListener(true))
            r2FXLLayout.addOnTapListener(object : R2FXLLayout.OnTapListener {
                override fun onTap(view: R2FXLLayout, info: R2FXLLayout.TapInfo): Boolean {
                    return webview.listener?.onTap(PointF(info.x, info.y)) ?: false
                }
            })

            return view
        }
    }

    override fun onDetach() {
        super.onDetach()

        // Prevent the web views from leaking when their parent is detached.
        // See https://stackoverflow.com/a/19391512/1474476
        for (wv in webViews) {
            (wv.parent as? ViewGroup)?.removeView(wv)
            wv.removeAllViews()
            wv.destroy()
        }
    }

    override fun onDestroyView() {
        for (webView in webViews) {
            webView.listener = null
        }
        _singleBinding = null
        _doubleBinding = null
        _singleLandscapeBinding = null
        _doubleLandscapeBinding = null
        super.onDestroyView()
    }

    fun isLandscape(): Boolean {
        return requireActivity().resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    fun getNavigationBarHeightByInsets(activity: Activity): Int {
        val insets = activity.window.decorView.rootWindowInsets
        if (insets != null) {
            return insets.systemWindowInsetBottom
        }
        return 0
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(webView: R2BasicWebView, link: Link?, resourceUrl: Url?, doubleLeft: Boolean? = null) {
        webViews.add(webView)
        navigator?.let {
            webView.listener = it.webViewListener
        }

        webView.settings.javaScriptEnabled = true
        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false
        webView.settings.useWideViewPort = true
        webView.settings.loadWithOverviewMode = true
        webView.settings.setSupportZoom(true)
        webView.settings.builtInZoomControls = true
        webView.settings.displayZoomControls = false
        // If we don't explicitly override the [textZoom], it will be set by Android's
        // accessibility font size system setting which breaks the layout of some fixed layouts.
        // See https://github.com/readium/kotlin-toolkit/issues/76
        webView.settings.textZoom = 100

        webView.setInitialScale(1)

        webView.setPadding(0, 0, 0, 0)
        webView.addJavascriptInterface(webView, "Android")

        webView.webViewClient = object : WebViewClientCompat() {

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                (webView as? R2BasicWebView)?.shouldOverrideUrlLoading(request) ?: false

            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                (webView as? R2BasicWebView)?.shouldInterceptRequest(view, request, doubleLeft)

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                if (link != null) {
                    webView.listener?.onResourceLoaded(webView, link)
                    webView.listener?.onPageLoaded(webView, link)
                }
            }
        }
        webView.isHapticFeedbackEnabled = false
        webView.isLongClickable = false
        webView.setOnLongClickListener {
            true
        }

        resourceUrl?.let { webView.loadUrl(it.toString()) }
    }

    companion object {

        fun newInstance(left: Pair<Link, Url>?, right: Pair<Link, Url>? = null): R2FXLPageFragment =
            R2FXLPageFragment().apply {
                arguments = Bundle().apply {
                    putParcelable("firstLink", left?.first)
                    putParcelable("firstUrl", left?.second)
                    putParcelable("secondLink", right?.first)
                    putParcelable("secondUrl", right?.second)
                }
            }
    }
}
