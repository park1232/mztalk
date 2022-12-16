package com.mztalk.mentor.service.impl;

import com.mztalk.mentor.domain.SearchCondition;
import com.mztalk.mentor.domain.Status;
import com.mztalk.mentor.domain.dto.BoardDto;
import com.mztalk.mentor.domain.entity.Board;
import com.mztalk.mentor.domain.entity.Mentor;
import com.mztalk.mentor.domain.entity.Result;
import com.mztalk.mentor.exception.BoardNotFoundException;
import com.mztalk.mentor.repository.BoardRepository;
import com.mztalk.mentor.repository.MentorRepository;
import com.mztalk.mentor.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardServiceImpl implements BoardService {

    private final BoardRepository boardRepository;
    private final MentorRepository mentorRepository;

    @Override
    @Transactional
    public Long saveBoard(ConcurrentHashMap<String,String> boardDto) {
        Long userId = Long.parseLong(boardDto.get("userId"));
        Mentor mentor = mentorRepository.findMentorByUserId(userId);
        Board board = Board.builder().
                category(boardDto.get("category")).
                title(boardDto.get("title")).
                nickname(boardDto.get("nickname")).
                content(boardDto.get("content")).
                introduction(boardDto.get("introduction")).
                career(boardDto.get("career")).
                salary(Integer.parseInt(boardDto.get("salary"))).
                status(Status.YES).
                build();
        board.addMentor(mentor);
        return boardRepository.save(board).getId();
    }

    @Override
    public BoardDto findById(Long id) {
        Board board = boardRepository.findById(id).orElseThrow(() -> new BoardNotFoundException("해당 번호의 글이 존재하지 않습니다."));
        BoardDto boardDto = new BoardDto(board);
        return boardDto;
    }

    @Override
    public Result findBoardByUserId(Long userId) {
        List<Board> boardList = boardRepository.findBoardByUserId(userId);
        List<BoardDto> collect = boardList.stream().map(BoardDto::new).collect(Collectors.toList());
        return new Result(collect);
    }

    @Override
    public Result findAll() {
        List<Board> boards = boardRepository.findAll();
        List<BoardDto> collect = boards.stream().map(BoardDto::new).collect(Collectors.toList());
        return new Result(collect);
    }

    @Override
    @Transactional
    public Long delete(Long id) {
        Board board = boardRepository.findById(id).orElseThrow(() -> new BoardNotFoundException("해당 번호의 글이 존재하지 않습니다."));
        board.changeStatus();
        return board.getId();
    }

    @Override
    @Transactional
    public Long updateBoard(Long id, BoardDto boardDto) {
        Board savedBoard = boardRepository.findById(id).orElseThrow(() -> new BoardNotFoundException("해당 번호의 글이 존재하지 않습니다."));
        savedBoard.updateBoard(boardDto);
        return savedBoard.getId();
    }

    @Override
    public Result searchWithCondition(SearchCondition searchCondition) {
        System.out.println(searchCondition.toString());
        List<Board> boardList = boardRepository.searchWithCondition(searchCondition);
        System.out.println("boardList = " + boardList.toString());
        List<BoardDto> collect = boardList.stream().map(BoardDto::new).collect(Collectors.toList());
        return new Result(collect);
    }
}
