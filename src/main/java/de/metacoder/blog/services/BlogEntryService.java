package de.metacoder.blog.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.metacoder.blog.Caches;
import de.metacoder.blog.persistence.entities.BlogEntry;
import de.metacoder.blog.persistence.repositories.BlogEntryRepository;
import de.metacoder.blog.persistence.repositories.UserRepository;
import de.metacoder.blog.transferobjects.BlogEntryCommentTO;
import de.metacoder.blog.transferobjects.BlogEntryTO;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Service
@Cacheable(Caches.BLOG_ENTRY_TOS)
@Controller
//@Transactional // todo check if tx is opened when cache contains value
public class BlogEntryService {
	
	@Autowired
	private BlogEntryRepository blogEntryRepository;
	
	@Autowired
	private UserRepository userRepository;
	
	@Cacheable(value=Caches.BLOG_ENTRY_TOS, key="'allBlogEntriesOrderedByCreationDate'")
	public List<BlogEntryTO> getAllOrderedByCreationDate() {
		final List<BlogEntryTO> listToReturn = new ArrayList<>();
		
		for(BlogEntry entry : blogEntryRepository.getAllOrderedByCreationDate()){
			listToReturn.add(new BlogEntryTO(entry));
		}
		
		return listToReturn;
	}
	
	@Cacheable(value=Caches.BLOG_ENTRY_TOS, key="'page'.concat(#pageNumber).concat('size').concat(#pageSize)")
    @RequestMapping(value="/entries", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
	public List<BlogEntryTO> getAllBlogEntriesOrderedByDate(int pageSize, int pageNumber){

		final PageRequest pageRequest = new PageRequest(pageNumber, pageSize, new Sort(Direction.DESC, "creationDate"));
		
		final List<BlogEntryTO> listToReturn = new ArrayList<>();
		
		for(BlogEntry entry : blogEntryRepository.findAll(pageRequest).getContent()){
			listToReturn.add(new BlogEntryTO(entry));
		}
		
		return listToReturn;
	}

	@Cacheable(value=Caches.BLOG_ENTRY_TOS, key="'numRows'")
    @RequestMapping(value="/numRows", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
	public long numRows() {
		return blogEntryRepository.count();
	}

	@Cacheable(value=Caches.BLOG_ENTRY_TOS, key="'blogEntry'.concat(#entryId)")
	public BlogEntryTO getEntry(Long entryId) {
		return new BlogEntryTO(blogEntryRepository.findOne(entryId));
	}

	@Transactional
	@CacheEvict(value={Caches.BLOG_ENTRY_TOS, Caches.GLOBAL_PAGE_CACHE}, allEntries=true)
	public void addComment(BlogEntryTO entry, BlogEntryCommentTO newComment) {
		BlogEntry blogEntry = blogEntryRepository.findOne(entry.getId());
		blogEntry.getComments().add(newComment.toBlogEntryComment());
		blogEntryRepository.save(blogEntry);
	}

	@Transactional
	@CacheEvict(value={Caches.BLOG_ENTRY_TOS, Caches.GLOBAL_PAGE_CACHE}, allEntries=true)
	public void createNewBlogEntry(BlogEntryTO blogEntry) {
		BlogEntry newEntry = new BlogEntry();

		newEntry.setAuthor(userRepository.findOne(blogEntry.getAuthor()));
		newEntry.setContent(blogEntry.getContent());
		newEntry.setTitle(blogEntry.getTitle());

		blogEntryRepository.save(newEntry);
	}
	
	@Transactional
	@CacheEvict(value={Caches.BLOG_ENTRY_TOS, Caches.GLOBAL_PAGE_CACHE}, allEntries=true)
	public void updateBlogEntry(BlogEntryTO blogEntryTO){
		BlogEntry blogEntry = blogEntryRepository.findOne(blogEntryTO.getId());
		blogEntry.setContent(blogEntryTO.getContent());
		blogEntry.setTitle(blogEntryTO.getTitle());
	}

	@Transactional
	@CacheEvict(value={Caches.BLOG_ENTRY_TOS, Caches.GLOBAL_PAGE_CACHE}, allEntries=true)
	public void deleteEntry(Long blogEntryId) {
		blogEntryRepository.delete(blogEntryId);
	}
	
}
