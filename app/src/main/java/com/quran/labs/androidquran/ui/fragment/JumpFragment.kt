package com.quran.labs.androidquran.ui.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager.LayoutParams
import android.view.inputmethod.EditorInfo
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AlertDialog.Builder
import androidx.fragment.app.DialogFragment
import com.quran.common.search.SearchTextUtil
import com.quran.data.core.QuranInfo
import com.quran.labs.androidquran.QuranApplication
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.data.Constants
import com.quran.labs.androidquran.ui.helpers.JumpDestination
import com.quran.labs.androidquran.util.QuranSettings
import com.quran.labs.androidquran.util.QuranUtils
import com.quran.labs.androidquran.view.ForceCompleteTextView
import dev.zacsweers.metro.Inject
import timber.log.Timber
import com.quran.mobile.common.ui.core.R as UiCoreR

/**
 * [DialogFragment] of a dialog for quickly selecting and jumping to a particular location in the
 * Quran. A location can be selected by page number or Surah/Ayah.
 */
class JumpFragment : DialogFragment() {

  @Inject
  lateinit var quranInfo: QuranInfo
  private var suppressJump: Boolean = false

  private lateinit var juzInput: ForceCompleteTextView
  private lateinit var suraInput: ForceCompleteTextView
  private lateinit var ayahInput: EditText
  private lateinit var pageInput: EditText

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val activity: Activity = requireActivity()
    val inflater = activity.layoutInflater

    @SuppressLint("InflateParams")
    val layout = inflater.inflate(R.layout.jump_dialog, null)

    val builder = Builder(activity, R.style.QuranDialogTheme)
    builder.setTitle(activity.getString(R.string.menu_jump))

    // Juz chooser
    juzInput = layout.findViewById(R.id.juz_spinner)
    val juzs = (1..Constants.JUZ2_COUNT).map { juz ->
      activity.getString(R.string.juz2_description, QuranUtils.getLocalizedNumber(juz))
    }
    val juzAdapter = InfixFilterArrayAdapter(
      activity,
      android.R.layout.simple_spinner_dropdown_item,
      juzs
    )
    juzInput.setAdapter(juzAdapter)

    // Sura chooser
    suraInput = layout.findViewById(R.id.sura_spinner)
    val suras = activity.resources.getStringArray(UiCoreR.array.sura_names)
        .mapIndexed { index: Int, sura: String? ->
          QuranUtils.getLocalizedNumber(index + 1) + ". " + sura
        }

    val suraAdapter = InfixFilterArrayAdapter(
        activity,
        android.R.layout.simple_spinner_dropdown_item, suras
    )
    suraInput.setAdapter(suraAdapter)

    // Ayah chooser
    ayahInput = layout.findViewById(R.id.ayah_spinner)

    // Page chooser
    pageInput = layout.findViewById(R.id.page_number)
    pageInput.setOnEditorActionListener { _: TextView?, actionId: Int, _: KeyEvent? ->
      if (actionId == EditorInfo.IME_ACTION_GO) {
        dismiss()
        onSubmit()
        true
      } else {
        false
      }
    }

    pageInput.addTextChangedListener(object : TextWatcher {
      override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

      override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

      override fun afterTextChanged(s: Editable?) {
        if (suppressJump) return
        val number = s.toString().toIntOrNull() ?: return
        val pageNumber = number.coerceIn(1..quranInfo.numberOfPages)
        val sura = quranInfo.getSuraOnPage(pageNumber)
        val ayah = quranInfo.getFirstAyahOnPage(pageNumber)
        val juz = quranInfo.getJuzFromPage(pageNumber).coerceIn(1, juzs.size)

        suppressJump = true
        juzInput.tag = juz
        juzInput.setText(juzs[juz - 1], false)
        suraInput.setText(suras[sura - 1], false)
        suraInput.tag = sura
        ayahInput.setText(ayah.toString())
        suppressJump = false
      }
    })

    juzInput.setOnForceCompleteListener { _: ForceCompleteTextView?, position: Int, _: Long ->
      val enteredText = juzInput.text.toString()
      val juzName: String? = when {
        position >= 0 -> juzAdapter.getItem(position)
        juzs.contains(enteredText) -> enteredText
        juzAdapter.isEmpty -> null
        else -> juzAdapter.getItem(0)
      }

      val juz = (juzs.indexOf(juzName) + 1).let { if (it <= 0) 1 else it }
      if (position < 0 && juz == juzInput.tag as? Int) {
        suppressJump = true
        juzInput.setText(juzs[juz - 1], false)
        suppressJump = false
        return@setOnForceCompleteListener
      }

      val page = quranInfo.getStartingPageForJuz(juz).coerceIn(1..quranInfo.numberOfPages)
      val sura = quranInfo.getSuraOnPage(page)
      val ayah = quranInfo.getFirstAyahOnPage(page)

      suppressJump = true
      juzInput.tag = juz
      juzInput.setText(juzs[juz - 1], false)
      suraInput.tag = sura
      suraInput.setText(suras[sura - 1], false)
      ayahInput.tag = ayah
      ayahInput.setText(ayah.toString())
      pageInput.hint = QuranUtils.getLocalizedNumber(page)
      pageInput.text = null
      suppressJump = false
    }

    suraInput.setOnForceCompleteListener { _: ForceCompleteTextView?, position: Int, _: Long ->
      val enteredText = suraInput.text.toString()

      val suraName: String? = when {
        // user selects
        position >= 0 -> { suraAdapter.getItem(position) }
        suras.contains(enteredText) -> { enteredText }
        // leave to the next code
        suraAdapter.isEmpty -> { null }
        // maybe first initialization or invalid input
        else -> { suraAdapter.getItem(0) }
      }

      var sura = suras.indexOf(suraName) + 1
      // default to al-Fatiha
      if (sura == 0) sura = 1
      val previousSura = suraInput.tag as? Int
      suraInput.tag = sura
      suraInput.setText(suras[sura - 1], false)
      if (position < 0 && sura == previousSura) {
        return@setOnForceCompleteListener
      }

      // trigger ayah change
      val ayahValue: CharSequence = ayahInput.text
      // space is intentional, to differentiate with value set by the user (delete/backspace)
      ayahInput.setText(ayahValue.ifEmpty { " " })
    }

    ayahInput.addTextChangedListener(object : TextWatcher {
      override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

      override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

      override fun afterTextChanged(s: Editable) {
        val ayahString = s.toString()
        var ayah = ayahString.toIntOrNull() ?: 1
        if (suppressJump) {
          ayahInput.tag = ayah
        } else {
          val suraTag = suraInput.tag
          if (suraTag != null) {
            val sura = suraTag as Int
            val ayahCount = quranInfo.getNumberOfAyahs(sura)
            // ensure in 1..ayahCount
            ayah = ayah.coerceIn(1..ayahCount)
            val page = quranInfo.getPageFromSuraAyah(sura, ayah)
            val juz = quranInfo.getJuzFromPage(page).coerceIn(1, juzs.size)
            suppressJump = true
            pageInput.hint = QuranUtils.getLocalizedNumber(page)
            pageInput.text = null
            juzInput.tag = juz
            juzInput.setText(juzs[juz - 1], false)
            suppressJump = false
          }
          ayahInput.tag = ayah
          // seems numeric IM always use western arabic (not localized)
          val correctText = ayah.toString()
          // empty input means the user clears the input, we don't force to fill it, let him type
          if (s.isNotEmpty() && correctText != ayahString) {
            s.replace(0, s.length, correctText)
          }
        }
      }
    })

    val settings = QuranSettings.getInstance(activity)
    val argPage = arguments?.getInt(ARG_PAGE, Constants.NO_PAGE) ?: Constants.NO_PAGE
    val argSura = arguments?.getInt(ARG_SURA, -1) ?: -1
    val argAyah = arguments?.getInt(ARG_AYAH, -1) ?: -1

    val resolvedPage = when {
      argPage in 1..quranInfo.numberOfPages -> argPage
      argSura > 0 && argAyah > 0 -> quranInfo.getPageFromSuraAyah(argSura, argAyah)
      settings.automaticLastPage in 1..quranInfo.numberOfPages -> settings.automaticLastPage
      settings.lastPage in 1..quranInfo.numberOfPages -> settings.lastPage
      else -> 1
    }.coerceIn(1..quranInfo.numberOfPages)

    val resolvedSura = when {
      argSura in 1..Constants.SURAS_COUNT -> argSura
      settings.automaticLastPage == resolvedPage &&
          settings.automaticLastSura in 1..Constants.SURAS_COUNT -> settings.automaticLastSura
      else -> quranInfo.getSuraOnPage(resolvedPage)
    }

    val resolvedAyah = when {
      argAyah > 0 -> argAyah
      settings.automaticLastPage == resolvedPage && settings.automaticLastAyah > 0 ->
        settings.automaticLastAyah
      else -> quranInfo.getFirstAyahOnPage(resolvedPage)
    }.coerceIn(1..quranInfo.getNumberOfAyahs(resolvedSura))

    val resolvedJuz = quranInfo.getJuzFromPage(resolvedPage).coerceIn(1, juzs.size)

    suppressJump = true
    pageInput.hint = QuranUtils.getLocalizedNumber(resolvedPage)
    juzInput.tag = resolvedJuz
    juzInput.setText(juzs[resolvedJuz - 1], false)
    suraInput.tag = resolvedSura
    suraInput.setText(suras[resolvedSura - 1], false)
    ayahInput.tag = resolvedAyah
    ayahInput.setText(resolvedAyah.toString())
    suppressJump = false

    builder.setView(layout)
    builder.setPositiveButton(
        getString(R.string.dialog_ok)
    ) { _: DialogInterface?, _: Int ->
      // trigger sura completion
      layout.requestFocus()
      dismiss()
      onSubmit()
    }
    return builder.create()
  }

  private fun onSubmit() {
    try {
      val pageStr = pageInput.text.toString()
      val page = if (pageStr.isEmpty()) {
        pageInput.hint.toString().toIntOrNull()
      } else {
        pageStr.toIntOrNull()
      }

      if (page != null) {
        val selectedSura = suraInput.tag as Int
        val selectedAyah = ayahInput.tag as Int
        (activity as? JumpDestination)?.jumpToAndHighlight(page, selectedSura, selectedAyah)
      }
    } catch (e: Exception) {
      Timber.d(e, "Could not jump, something went wrong...")
    }
  }

  override fun onAttach(context: Context) {
    super.onAttach(context)
    (context.applicationContext as QuranApplication).applicationComponent
        .inject(this)
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)
    dialog?.window?.setSoftInputMode(
        LayoutParams.SOFT_INPUT_STATE_VISIBLE or LayoutParams.SOFT_INPUT_ADJUST_PAN
    )
  }

  /**
   * ListAdapter that supports filtering by using case-insensitive infix (substring).
   */
  private class InfixFilterArrayAdapter(
    context: Context,
    @LayoutRes private val itemLayoutRes: Int,
    private val originalItems: List<String>
  ) : BaseAdapter(), Filterable {
    private var items: List<String>
    private val inflater: LayoutInflater
    private val filter: Filter = ItemFilter()
    private val isRtl = SearchTextUtil.isRtl(originalItems.first())
    private val searchPreparedItems = originalItems.map { prepareForSearch(it, isRtl) }

    init {
      this.items = originalItems
      inflater = LayoutInflater.from(context)
    }

    override fun getCount() = items.size

    override fun getItem(position: Int) = items[position]

    override fun getItemId(position: Int) = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
      val view = convertView ?: inflater.inflate(itemLayoutRes, parent, false)

      // As no fieldId is known/assigned, assume it is a TextView
      val text = view as TextView
      text.text = getItem(position)
      return view
    }

    override fun getFilter() = filter

    private fun prepareForSearch(input: String, isRtl: Boolean): String {
      return SearchTextUtil.asSearchableString(input, isRtl)
    }

    /**
     * Filter that do filtering by matching case-insensitive infix of the input.
     */
    private inner class ItemFilter : Filter() {
      override fun performFiltering(constraint: CharSequence?): FilterResults {
        val results = FilterResults()

        // The items never change after construction, not sure if really needs to copy
        if (constraint == null || constraint.isEmpty()) {
          results.values = originalItems
          results.count = originalItems.size
        } else {
          val infix = cleanUpQueryString(constraint.toString())
          val filteredIndex = infix.toIntOrNull()?.toString()
          val filteredCopy = originalItems.filterIndexed { index, sura ->
            searchPreparedItems[index].contains(infix) ||
                // support English numbers in Arabic mode
                filteredIndex != null && (index + 1).toString().contains(filteredIndex)
          }
          results.values = filteredCopy
          results.count = filteredCopy.size
        }
        return results
      }

      private fun cleanUpQueryString(query: String): String {
        return if (SearchTextUtil.isRtl(query)) {
          SearchTextUtil.asSearchableString(query, true)
        } else {
          query.lowercase()
        }
      }

      override fun publishResults(constraint: CharSequence, results: FilterResults) {
        items = results.values as List<String>
        if (results.count > 0) {
          notifyDataSetChanged()
        } else {
          notifyDataSetInvalidated()
        }
      }
    }
  }

  companion object {
    const val TAG = "JumpFragment"
    private const val ARG_PAGE = "arg_page"
    private const val ARG_SURA = "arg_sura"
    private const val ARG_AYAH = "arg_ayah"

    fun newInstance(
      initialPage: Int = Constants.NO_PAGE,
      initialSura: Int = -1,
      initialAyah: Int = -1
    ): JumpFragment {
      return JumpFragment().apply {
        arguments = Bundle().apply {
          putInt(ARG_PAGE, initialPage)
          putInt(ARG_SURA, initialSura)
          putInt(ARG_AYAH, initialAyah)
        }
      }
    }
  }
}
