package com.learning.english.unit;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.learning.english.dto.RepetitionAddRequest;
import com.learning.english.dto.RepetitionDisplayRequest;
import com.learning.english.dto.RepetitionDisplayResponse;
import com.learning.english.dto.RepetitionUpdateRequest;
import com.learning.english.models.*;
import com.learning.english.repository.GroupRepository;
import com.learning.english.repository.RepetitionRepository;
import com.learning.english.repository.RepetitionWordRepository;
import com.learning.english.repository.WordRepository;
import com.learning.english.service.RepetitionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.*;

@ExtendWith(MockitoExtension.class)
public class RepetitionServiceTest {

    @Mock
    private RepetitionWordRepository repetitionWordRepository;

    @Mock
    private WordRepository wordRepository;

    @Mock
    private RepetitionRepository repetitionRepository;

    @Mock
    private User user;

    @Mock
    private GroupRepository groupRepository;

    @InjectMocks
    private RepetitionService repetitionService;


    @Test
    public void testUpdateRepetition_validGrade() {
        RepetitionWord repetitionWord = mock(RepetitionWord.class);
        Repetition repetition = mock(Repetition.class);
        User user = mock(User.class);

        when(repetitionWordRepository.findById(anyInt())).thenReturn(Optional.of(repetitionWord));
        when(repetitionWord.getRepetition()).thenReturn(repetition);
        when(repetition.getStudent()).thenReturn(user);

        RepetitionUpdateRequest request = new RepetitionUpdateRequest(1, 2);

        repetitionService.updateRepetition(request, user);

        verify(repetitionWordRepository, times(1)).save(repetitionWord);

        verify(repetitionWord, times(1)).setNextReviewDate(any(LocalDate.class));
        verify(repetitionWord, times(1)).setInterval(anyInt());
    }

    @Test
    public void testUpdateRepetition_invalidGrade() {
        RepetitionWord repetitionWord = mock(RepetitionWord.class);
        Repetition repetition = mock(Repetition.class);
        User user = mock(User.class);

        when(repetitionWordRepository.findById(anyInt())).thenReturn(Optional.of(repetitionWord));
        when(repetitionWord.getRepetition()).thenReturn(repetition);
        when(repetition.getStudent()).thenReturn(user);

        RepetitionUpdateRequest request = new RepetitionUpdateRequest(1, -1);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            repetitionService.updateRepetition(request, user);
        });

        assertEquals("Invalid grade", exception.getMessage());
    }

    @Test
    public void testUpdateRepetition_userNotAuthorized() {
        RepetitionUpdateRequest request = new RepetitionUpdateRequest(1, 2);
        User wrongUser = mock(User.class);
        User correctUser = mock(User.class);

        RepetitionWord repetitionWord = mock(RepetitionWord.class);
        Repetition repetition = mock(Repetition.class);
        when(repetitionWordRepository.findById(anyInt())).thenReturn(Optional.of(repetitionWord));
        when(repetitionWord.getRepetition()).thenReturn(repetition);
        when(repetition.getStudent()).thenReturn(wrongUser);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            repetitionService.updateRepetition(request, correctUser);
        });

        assertEquals("User does not have permission to update this repetition word", exception.getMessage());
    }

    @Test
    public void testUpdateRepetition_notFound() {
        RepetitionUpdateRequest request = new RepetitionUpdateRequest(1, 2);

        when(repetitionWordRepository.findById(anyInt())).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            repetitionService.updateRepetition(request, mock(User.class));
        });

        assertEquals("Repetition word not found", exception.getMessage());
    }

    @Test
    public void testGetRepetitionWords_noAnsweredWordIds() {
        User user = mock(User.class);
        RepetitionDisplayRequest request = mock(RepetitionDisplayRequest.class);

        when(request.getLimit()).thenReturn(10);

        when(request.getAnsweredWordIds()).thenReturn(Collections.emptyList());

        RepetitionWord repetitionWord = mock(RepetitionWord.class);
        Word word = mock(Word.class);

        when(word.getWord()).thenReturn("Test");
        when(word.getTranslation()).thenReturn("Test Translation");
        when(word.getAudioFilePath()).thenReturn("audio/path");
        when(word.getImageFilePath()).thenReturn("image/path");

        when(repetitionWord.getWord()).thenReturn(word);
        when(repetitionWord.getId()).thenReturn(1);

        List<RepetitionWord> repetitionList = List.of(repetitionWord);
        when(repetitionWordRepository.findDueRepetitions(user.getId(), request.getGroupId(), PageRequest.of(0, 10)))
                .thenReturn(repetitionList);

        List<RepetitionDisplayResponse> responses = repetitionService.getRepetitionWords(user, request);

        assertEquals(1, responses.size());
        assertEquals("Test", responses.get(0).getWord());
        assertEquals("Test Translation", responses.get(0).getTranslation());
    }

    @Test
    public void testGetRepetitionWords_withAnsweredWordIds() {
        User user = mock(User.class);
        RepetitionDisplayRequest request = mock(RepetitionDisplayRequest.class);
        when(request.getLimit()).thenReturn(10);
        when(request.getAnsweredWordIds()).thenReturn(List.of(1));

        RepetitionWord repetitionWord2 = mock(RepetitionWord.class);
        Word word2 = mock(Word.class);
        when(word2.getWord()).thenReturn("Test 2");
        when(word2.getTranslation()).thenReturn("Test 2 Translation");
        when(word2.getAudioFilePath()).thenReturn("audio2/path");
        when(word2.getImageFilePath()).thenReturn("image2/path");
        when(repetitionWord2.getWord()).thenReturn(word2);
        when(repetitionWord2.getId()).thenReturn(2);

        when(repetitionWordRepository.findDueRepetitionsExcludingAnswered(
                user.getId(), request.getGroupId(), List.of(1), PageRequest.of(0, 10)))
                .thenReturn(List.of(repetitionWord2));

        List<RepetitionDisplayResponse> responses = repetitionService.getRepetitionWords(user, request);

        assertEquals(1, responses.size());
        assertEquals("Test 2", responses.get(0).getWord());
        assertEquals("Test 2 Translation", responses.get(0).getTranslation());
    }

    @Test
    public void testGetRepetitionWords_emptyResult() {
        User user = mock(User.class);
        RepetitionDisplayRequest request = mock(RepetitionDisplayRequest.class);
        when(request.getLimit()).thenReturn(10);
        when(request.getAnsweredWordIds()).thenReturn(null);

        when(repetitionWordRepository.findDueRepetitions(user.getId(), request.getGroupId(), PageRequest.of(0, 10)))
                .thenReturn(List.of());

        List<RepetitionDisplayResponse> responses = repetitionService.getRepetitionWords(user, request);

        assertTrue(responses.isEmpty());
    }

    @Test
    public void testIsWordInRepetitions_wordExists() {
        Long wordId = 1L;
        User user = mock(User.class);
        Word word = mock(Word.class);

        when(wordRepository.findById(wordId)).thenReturn(Optional.of(word));
        when(repetitionWordRepository.existsByRepetitionStudentAndWord(user, word)).thenReturn(true);

        boolean result = repetitionService.isWordInRepetitions(wordId, user);

        assertTrue(result);
        verify(wordRepository, times(1)).findById(wordId);
        verify(repetitionWordRepository, times(1)).existsByRepetitionStudentAndWord(user, word);
    }

    @Test
    public void testIsWordInRepetitions_wordNotExistsInRepetition() {
        Long wordId = 1L;
        User user = mock(User.class);
        Word word = mock(Word.class);

        when(wordRepository.findById(wordId)).thenReturn(Optional.of(word));
        when(repetitionWordRepository.existsByRepetitionStudentAndWord(user, word)).thenReturn(false);

        boolean result = repetitionService.isWordInRepetitions(wordId, user);

        assertFalse(result);
        verify(wordRepository, times(1)).findById(wordId);
        verify(repetitionWordRepository, times(1)).existsByRepetitionStudentAndWord(user, word);
    }

    @Test
    public void testIsWordInRepetitions_wordNotFound() {
        Long wordId = 1L;
        User user = mock(User.class);

        when(wordRepository.findById(wordId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            repetitionService.isWordInRepetitions(wordId, user);
        });

        assertEquals("Word not found", exception.getMessage());
        verify(wordRepository, times(1)).findById(wordId);
        verifyNoInteractions(repetitionWordRepository);
    }

    @Test
    public void testCountRepetitionsForTodayByGroup_repetitionsExist() {
        User user = mock(User.class);
        Integer groupId = 1;
        LocalDate today = LocalDate.now();
        long expectedCount = 5L;

        when(repetitionWordRepository.countByRepetitionStudentAndRepetitionGroupIdAndNextReviewDateLessThanEqual(user, groupId, today))
                .thenReturn(expectedCount);

        long result = repetitionService.countRepetitionsForTodayByGroup(user, groupId);

        assertEquals(expectedCount, result);
        verify(repetitionWordRepository, times(1))
                .countByRepetitionStudentAndRepetitionGroupIdAndNextReviewDateLessThanEqual(user, groupId, today);
    }

    @Test
    public void testCountRepetitionsForTodayByGroup_noRepetitions() {
        User user = mock(User.class);
        Integer groupId = 1;
        LocalDate today = LocalDate.now();

        when(repetitionWordRepository.countByRepetitionStudentAndRepetitionGroupIdAndNextReviewDateLessThanEqual(user, groupId, today))
                .thenReturn(0L);

        long result = repetitionService.countRepetitionsForTodayByGroup(user, groupId);

        assertEquals(0L, result);
        verify(repetitionWordRepository, times(1))
                .countByRepetitionStudentAndRepetitionGroupIdAndNextReviewDateLessThanEqual(user, groupId, today);
    }

    @Test
    public void testRemoveRepetition_successful() {
        Integer wordId = 1;
        User user = mock(User.class);
        RepetitionWord repetitionWord = mock(RepetitionWord.class);
        Repetition repetition = mock(Repetition.class);

        when(repetitionWordRepository.findByRepetition_StudentAndWord_Id(user, wordId))
                .thenReturn(Optional.of(repetitionWord));
        when(repetitionWord.getRepetition()).thenReturn(repetition);

        Set<RepetitionWord> repetitionWords = new HashSet<>();
        repetitionWords.add(repetitionWord);

        when(repetition.getRepetitionWords()).thenReturn(new ArrayList<>(repetitionWords));

        doNothing().when(repetitionWordRepository).delete(repetitionWord);

        repetitionService.removeRepetition(wordId, user);

        verify(repetitionWordRepository, times(1)).delete(repetitionWord);

        verify(repetitionRepository, times(1)).delete(repetition);
    }

    @Test
    public void testRemoveRepetition_wordNotFound() {
        Integer wordId = 1;
        User user = mock(User.class);

        when(repetitionWordRepository.findByRepetition_StudentAndWord_Id(user, wordId))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            repetitionService.removeRepetition(wordId, user);
        });

        assertEquals("Repetition word not found", exception.getMessage());
    }

    @Test
    public void testRemoveRepetition_noRepetitionLeft() {
        Integer wordId = 1;
        User user = mock(User.class);
        RepetitionWord repetitionWord = mock(RepetitionWord.class);
        Repetition repetition = mock(Repetition.class);

        when(repetitionWordRepository.findByRepetition_StudentAndWord_Id(user, wordId))
                .thenReturn(Optional.of(repetitionWord));
        when(repetitionWord.getRepetition()).thenReturn(repetition);

        List<RepetitionWord> repetitionWords = new ArrayList<>();
        repetitionWords.add(repetitionWord);
        when(repetition.getRepetitionWords()).thenReturn(repetitionWords);

        when(repetition.getRepetitionWords()).thenReturn(Collections.emptyList());

        doNothing().when(repetitionWordRepository).delete(repetitionWord);

        repetitionService.removeRepetition(wordId, user);

        verify(repetitionWordRepository, times(1)).delete(repetitionWord);

        verify(repetitionRepository, times(1)).delete(repetition);
    }

    @Test
    void testAddRepetition_wordNotFound() {
        RepetitionAddRequest request = new RepetitionAddRequest(1L, 1);
        when(wordRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            repetitionService.addRepetition(request, user);
        });
    }


    @Test
    void testAddRepetition_groupNotFound() {
        RepetitionAddRequest request = new RepetitionAddRequest(1L, 1);
        when(wordRepository.findById(1L)).thenReturn(Optional.of(new Word()));
        when(groupRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            repetitionService.addRepetition(request, user);
        });
    }


    @Test
    public void testAddRepetition_userNotInGroup() {
        RepetitionAddRequest request = new RepetitionAddRequest(1L, 1);
        Word word = new Word();
        Group group = mock(Group.class);
        group.setId(1);

        UserGroup userGroup = mock(UserGroup.class);
        when(userGroup.getUser()).thenReturn(user);

        when(wordRepository.findById(1L)).thenReturn(Optional.of(word));
        when(groupRepository.findById(1)).thenReturn(Optional.of(group));

        when(group.getUserGroups()).thenReturn(Collections.singletonList(userGroup));

        repetitionService.addRepetition(request, user);
    }

    @Test
    void testAddRepetition_repetitionAlreadyExists() {
        RepetitionAddRequest request = new RepetitionAddRequest(1L, 1);
        Word word = new Word();
        Group group = mock(Group.class);
        group.setId(1);

        UserGroup userGroup = mock(UserGroup.class);
        when(userGroup.getUser()).thenReturn(user);

        when(wordRepository.findById(1L)).thenReturn(Optional.of(word));
        when(groupRepository.findById(1)).thenReturn(Optional.of(group));
        when(group.getUserGroups()).thenReturn(Collections.singletonList(userGroup));

        when(repetitionWordRepository.existsByRepetitionStudentAndWord(user, word)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> {
            repetitionService.addRepetition(request, user);
        });
    }

    @Test
    public void testAddRepetition_success() {
        RepetitionAddRequest request = new RepetitionAddRequest(1L, 1);
        Word word = new Word();
        Group group = mock(Group.class);
        when(wordRepository.findById(1L)).thenReturn(Optional.of(word));
        when(groupRepository.findById(1)).thenReturn(Optional.of(group));

        UserGroup userGroup = mock(UserGroup.class);
        when(userGroup.getUser()).thenReturn(user);

        when(group.getUserGroups()).thenReturn(Collections.singletonList(userGroup));

        repetitionService.addRepetition(request, user);
    }
}

