package xyz.chz.bfm.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import xyz.chz.bfm.adapter.AppListAdapter
import xyz.chz.bfm.adapter.AppManager
import xyz.chz.bfm.data.AppInfo
import xyz.chz.bfm.databinding.FragmentAppListBinding
import java.text.Collator
import xyz.chz.bfm.util.*
import xyz.chz.bfm.util.command.TermUtil

@AndroidEntryPoint
class AppListFragment : Fragment() {

    private lateinit var binding: FragmentAppListBinding

    private var adapter: AppListAdapter? = null
    private var appsAll: List<AppInfo>? = null
    private val defaultsSharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(
            requireActivity()
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAppListBinding.inflate(layoutInflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            val dividerItemDecoration =
                DividerItemDecoration(requireActivity(), LinearLayoutManager.VERTICAL)
            rvApps.addItemDecoration(dividerItemDecoration)

            val applist = TermUtil.appidList

            AppManager.rxLoadNetworkAppList(requireActivity())
                .subscribeOn(Schedulers.io())
                .map {
                    if (applist != null) {
                        it.forEach { one ->
                            if ((applist.contains(one.packageName))) {
                                one.isSelected = 1
                            } else {
                                one.isSelected = 0
                            }
                        }
                        val comparator = Comparator<AppInfo> { p1, p2 ->
                            when {
                                p1.isSelected > p2.isSelected -> -1
                                p1.isSelected == p2.isSelected -> 0
                                else -> 1
                            }
                        }
                        it.sortedWith(comparator)
                    } else {
                        val comparator = object : Comparator<AppInfo> {
                            val collator = Collator.getInstance()
                            override fun compare(o1: AppInfo, o2: AppInfo) =
                                collator.compare(o1.appName, o2.appName)
                        }
                        it.sortedWith(comparator)
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    appsAll = it
                    adapter = AppListAdapter(requireActivity(), it, applist)
                    rvApps.adapter = adapter
                    prgWaiting.visibility = View.GONE
                }
            switchBypassApps.setOnCheckedChangeListener { _, isChecked ->
                TermUtil.setWhitelistOrBlacklist(isChecked)
            }
            switchBypassApps.isChecked = TermUtil.isBlackListMode
        }
    }

    override fun onPause() {
        super.onPause()
        adapter?.let {
            TermUtil.setAppidList(it.blacklist)
        }
    }

}