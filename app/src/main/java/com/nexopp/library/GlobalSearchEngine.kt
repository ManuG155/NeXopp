package com.nexopp.library

/**
 * Interface for pluggable content search providers (e.g. OCR, PDF indexer, handwriting engine, LaTeX matcher).
 */
interface ContentSearchProvider {
    fun searchContent(query: String, notebook: Notebook): List<SearchResultItem>
}

/**
 * Fast, extensible Global Search Engine for NeXopp notes and library.
 * Performs fast multi-attribute matching (Name, Subject, Tags) and queries attached content providers.
 */
class GlobalSearchEngine(
    private val contentProviders: List<ContentSearchProvider> = emptyList()
) {

    /**
     * Executes a search query across the provided library data.
     * @param query Raw search query text.
     * @param notebooks Complete list of notebooks.
     * @param subjects Available subjects.
     * @param tags Available tags.
     * @param filterSubjectId Optional subject filter.
     * @param filterTagIds Optional tag filters (matches if notebook contains ANY or ALL selected tags).
     * @param onlyFavorites Filter to favorites only if true.
     */
    fun search(
        query: String,
        notebooks: List<Notebook>,
        subjects: List<Subject>,
        tags: List<Tag>,
        filterSubjectId: String? = null,
        filterTagIds: Set<String> = emptySet(),
        onlyFavorites: Boolean = false,
        sortOption: NotebookSortOption = NotebookSortOption.RECENT_DESC
    ): List<SearchResultItem> {
        val cleanQuery = query.trim()
        val subjectsMap = subjects.associateBy { it.id }
        val tagsMap = tags.associateBy { it.id }

        // Filter by base criteria first (Subject, Tags, Favorites)
        val candidateNotebooks = notebooks.filter { nb ->
            if (onlyFavorites && !nb.isFavorite) return@filter false
            if (filterSubjectId != null && nb.subjectId != filterSubjectId) return@filter false
            if (filterTagIds.isNotEmpty() && !nb.tagIds.any { it in filterTagIds }) return@filter false
            true
        }

        if (cleanQuery.isEmpty()) {
            // Return all matching filter criteria ordered by sortOption
            return sortNotebooks(candidateNotebooks, sortOption).map { nb ->
                val subject = subjectsMap[nb.subjectId]
                val nbTags = nb.tagIds.mapNotNull { tagsMap[it] }
                SearchResultItem(
                    notebook = nb,
                    subject = subject,
                    matchedTags = nbTags,
                    matchKind = SearchMatchKind.NOTEBOOK_NAME,
                    matchSnippet = nb.name
                )
            }
        }

        val results = mutableListOf<SearchResultItem>()
        val seenNotebookIds = mutableSetOf<String>()

        for (nb in candidateNotebooks) {
            val subject = subjectsMap[nb.subjectId]
            val nbTags = nb.tagIds.mapNotNull { tagsMap[it] }

            // 1. Check Notebook Name match (Highest priority)
            if (nb.name.contains(cleanQuery, ignoreCase = true)) {
                results.add(
                    SearchResultItem(
                        notebook = nb,
                        subject = subject,
                        matchedTags = nbTags,
                        matchKind = SearchMatchKind.NOTEBOOK_NAME,
                        matchSnippet = nb.name
                    )
                )
                seenNotebookIds.add(nb.id)
                continue
            }

            // 2. Check Tag matches
            val matchingTags = nbTags.filter { it.name.contains(cleanQuery, ignoreCase = true) }
            if (matchingTags.isNotEmpty()) {
                results.add(
                    SearchResultItem(
                        notebook = nb,
                        subject = subject,
                        matchedTags = nbTags,
                        matchKind = SearchMatchKind.TAG_NAME,
                        matchSnippet = "Etiqueta: ${matchingTags.joinToString { it.name }}"
                    )
                )
                seenNotebookIds.add(nb.id)
                continue
            }

            // 3. Check Subject Name match
            if (subject != null && subject.name.contains(cleanQuery, ignoreCase = true)) {
                results.add(
                    SearchResultItem(
                        notebook = nb,
                        subject = subject,
                        matchedTags = nbTags,
                        matchKind = SearchMatchKind.SUBJECT_NAME,
                        matchSnippet = "Asignatura: ${subject.name}"
                    )
                )
                seenNotebookIds.add(nb.id)
                continue
            }

            // 4. Query any registered Content Search Providers (OCR, PDF, LaTeX, Strokes)
            for (provider in contentProviders) {
                val providerResults = provider.searchContent(cleanQuery, nb)
                if (providerResults.isNotEmpty()) {
                    results.addAll(providerResults)
                    seenNotebookIds.add(nb.id)
                    break
                }
            }
        }

        return results
    }

    private val collator = java.text.Collator.getInstance(java.util.Locale.getDefault()).apply {
        strength = java.text.Collator.PRIMARY
    }

    /**
     * Sorts notebooks according to the selected [NotebookSortOption].
     */
    fun sortNotebooks(
        notebooks: List<Notebook>,
        sortOption: NotebookSortOption
    ): List<Notebook> {
        return when (sortOption) {
            NotebookSortOption.RECENT_DESC -> notebooks.sortedByDescending { it.lastModified }
            NotebookSortOption.RECENT_ASC -> notebooks.sortedBy { it.lastModified }
            NotebookSortOption.NAME_ASC -> notebooks.sortedWith { a, b -> collator.compare(a.name, b.name) }
            NotebookSortOption.NAME_DESC -> notebooks.sortedWith { a, b -> collator.compare(b.name, a.name) }
            NotebookSortOption.PAGE_COUNT_DESC -> notebooks.sortedByDescending { it.pageCount }
            NotebookSortOption.PAGE_COUNT_ASC -> notebooks.sortedBy { it.pageCount }
        }
    }
}

