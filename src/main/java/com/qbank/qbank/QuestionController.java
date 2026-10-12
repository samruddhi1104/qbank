package com.qbank.qbank;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class QuestionController {

    private final QuestionRepository repo;

    public QuestionController(QuestionRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/questions";
    }

    @GetMapping("/questions")
    public String list(Model model) {
        model.addAttribute("questions", repo.findAll());
        model.addAttribute("question", new Question());
        return "questions";
    }

    @PostMapping("/questions")
    public String save(@Valid @ModelAttribute("question") Question question,
                       BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("questions", repo.findAll());
            return "questions";
        }
        repo.save(question);
        return "redirect:/questions";
    }
}