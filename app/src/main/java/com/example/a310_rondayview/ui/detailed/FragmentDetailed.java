package com.example.a310_rondayview.ui.detailed;


import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.a310_rondayview.R;
import com.example.a310_rondayview.data.event.EventDatabaseService;
import com.example.a310_rondayview.data.event.EventsFirestoreManager;
import com.example.a310_rondayview.model.Comment;
import com.example.a310_rondayview.model.CurrentEventSingleton;
import com.example.a310_rondayview.model.Event;
import com.example.a310_rondayview.ui.adapter.SimilarEventAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Collections;
import java.util.List;

public class FragmentDetailed extends Fragment {
    private class ViewHolder {
        ImageView eventImage;
        ImageView backImage;
        TextView clubNameText;
        TextView eventNameText;
        ImageView profileImage;
        TextView privacyStatusText;
        TextView locationText;
        TextView eventDateText;
        TextView eventDescText;
        RecyclerView similarEventRv;
        TextView addCommentText;
        EditText commentEditText;
        LinearLayout commentsLayout;
        RatingBar ratingBar;

        public ViewHolder(View view) {
            eventImage = view.findViewById(R.id.event_image);
            backImage = view.findViewById(R.id.back);
            clubNameText = view.findViewById(R.id.clubNameTextView);
            profileImage = view.findViewById(R.id.profileImageView);
            eventNameText = view.findViewById(R.id.event_name);
            eventDateText = view.findViewById(R.id.event_date);
            privacyStatusText = view.findViewById(R.id.event_privacy);
            locationText = view.findViewById(R.id.locationtext);
            eventDescText = view.findViewById(R.id.event_desc);
            similarEventRv = view.findViewById(R.id.similar_events_rv);
            addCommentText = view.findViewById(R.id.add_comment_text);
            commentEditText = view.findViewById(R.id.comment_edit_text);
            commentsLayout = view.findViewById(R.id.comments_layout);
            ratingBar = view.findViewById(R.id.rating_bar);
        }
    }
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    List<Event> similarEvents;
    CurrentEventSingleton currentEvent;
    ViewHolder vh;

    public static final int MATCH_PARENT = LinearLayout.LayoutParams.MATCH_PARENT;
    public static final int WRAP_CONTENT = LinearLayout.LayoutParams.WRAP_CONTENT;

    public FragmentDetailed() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        View view = inflater.inflate(R.layout.fragment_detailed, container, false);
        vh = new ViewHolder(view);
        currentEvent = CurrentEventSingleton.getInstance();
        vh.clubNameText.setText(currentEvent.getCurrentEvent().getClubName());
        vh.eventNameText.setText(currentEvent.getCurrentEvent().getTitle());
        String groupName = currentEvent.getCurrentEvent().getGroupNameTag();
        if(groupName==null||groupName.equals("")){
            vh.privacyStatusText.setText("Public event");
        } else {
            vh.privacyStatusText.setText("Group: "+groupName);
        }
        vh.eventDateText.setText(currentEvent.getCurrentEvent().getDateTime().toString());
        vh.locationText.setText(currentEvent.getCurrentEvent().getLocation());
        vh.eventDescText.setText(currentEvent.getCurrentEvent().getDescription());
        vh.addCommentText.setOnClickListener(v -> {
            String commentText = vh.commentEditText.getText().toString();
            float userRating = vh.ratingBar.getRating();
            if (!commentText.isEmpty()) {
                DocumentReference docRef = db.collection("users").document(mAuth.getCurrentUser().getUid());
                docRef.get().addOnSuccessListener(documentSnapshot -> {
                    String currentUsername = removeAtGmail(documentSnapshot.getString("email"));
                    Comment comment = new Comment(currentUsername, commentText, userRating);
                    currentEvent.getCurrentEvent().addComment(comment);
                    EventsFirestoreManager.getInstance().updateEvent(currentEvent.getCurrentEvent());
                    addComment(comment);
                });

                // Clear the EditText and the ratingBar after adding the comment
                vh.ratingBar.setRating(5);
                vh.commentEditText.setText("");
            }
        });
        if (currentEvent.getCurrentEvent().getComments() != null) {
            for (Comment comment : currentEvent.getCurrentEvent().getComments()) {
                addComment(comment);
            }
        }



        Glide.with(getContext()).load(currentEvent.getCurrentEvent().getImageURL()).into(vh.eventImage);
        Glide.with(getContext()).load(currentEvent.getCurrentEvent().getEventClubProfilePicture()).into(vh.profileImage);
        vh.backImage.setOnClickListener(v -> getActivity().getSupportFragmentManager().popBackStack());

        // load in the events from the database and sort them based on their similarity to
        // the current event dispalyed. (show a max of 10 events)
        EventDatabaseService eventDatabaseService = new EventDatabaseService();
        eventDatabaseService.getAllEvents().thenAccept(events -> {
            events.remove(currentEvent.getCurrentEvent());
            similarEvents = events;
            // setup the similar event recycler view
            FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
            vh.similarEventRv.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
            SimilarEventAdapter similarEventAdapter = new SimilarEventAdapter(getContext(), similarEvents, fragmentManager);
            vh.similarEventRv.setAdapter(similarEventAdapter);
            // sort based on similarity
            Collections.sort(similarEvents, new SimilarEventComparator(currentEvent.getCurrentEvent()));
        });

        return view;
    }
    private void addComment(Comment comment){
        int matchParent = MATCH_PARENT;
        int wrapContent = WRAP_CONTENT;
        float rating = comment.getRating();
        LinearLayout commentLayout = new LinearLayout(getContext());
        commentLayout.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                matchParent, wrapContent);

        LinearLayout ratingLayout = new LinearLayout(getContext());
        ratingLayout.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams ratingLayoutParams = new LinearLayout.LayoutParams(300, 100);

        layoutParams.setMargins(0, 20, 0, 20);

        //create an image view for stars of rating
        ImageView ratingStar = new ImageView(getContext());
        //setting image for stars according to the rating from the user
        if (rating == 0){
            ratingStar.setImageResource(R.drawable.no_star);
        }else if (rating == 0.5){
            ratingStar.setImageResource(R.drawable.half_star);
        }else if (rating == 1){
            ratingStar.setImageResource(R.drawable.one_star);
        }else if (rating == 1.5){
            ratingStar.setImageResource(R.drawable.one_half_star);
        }else if (rating == 2){
            ratingStar.setImageResource(R.drawable.two_star);
        }else if (rating == 2.5){
            ratingStar.setImageResource(R.drawable.two_half_star);
        }else if (rating == 3){
            ratingStar.setImageResource(R.drawable.three_star);
        }else if (rating == 3.5){
            ratingStar.setImageResource(R.drawable.three_half_star);
        }else if (rating == 4){
            ratingStar.setImageResource(R.drawable.four_star);
        }else if (rating == 4.5){
            ratingStar.setImageResource(R.drawable.four_half_star);
        }else if (rating == 5){
            ratingStar.setImageResource(R.drawable.five_star);
        }
        ratingStar.setLayoutParams(new LinearLayout.LayoutParams(matchParent, wrapContent));

        TextView commentTextView = new TextView(getContext());
        commentTextView.setTag(comment);
        commentTextView.setText(comment.getUsername() + ": " + comment.getCommentText());
        commentTextView.setTextColor(ContextCompat.getColor(getContext(), R.color.black));
        commentTextView.setTextSize(16);
        commentTextView.setLayoutParams(new LinearLayout.LayoutParams(
                0, wrapContent, 1
        ));

        // Create a "delete" button (CardView) for each comment
        CardView deleteButton = new CardView(getContext());
        deleteButton.setLayoutParams(new LinearLayout.LayoutParams(
                wrapContent, wrapContent
        ));
        deleteButton.setCardBackgroundColor(Color.RED);
        deleteButton.setCardElevation(8);

        TextView deleteTextView = new TextView(getContext());
        deleteTextView.setText("Delete");
        deleteTextView.setTextColor(Color.WHITE);
        deleteTextView.setGravity(Gravity.CENTER);
        deleteButton.addView(deleteTextView);


        deleteButton.setOnClickListener(v -> {
            currentEvent.getCurrentEvent().deleteComment(comment);
            EventsFirestoreManager.getInstance().updateEvent(currentEvent.getCurrentEvent());
            vh.commentsLayout.removeView(commentLayout);
            vh.commentsLayout.removeView(ratingLayout);
        });
        commentLayout.addView(commentTextView);
        commentLayout.addView(deleteButton);
        ratingLayout.addView(ratingStar);
        deleteButton.setVisibility(View.INVISIBLE);
        DocumentReference docRef = db.collection("users").document(mAuth.getCurrentUser().getUid());
        docRef.get().addOnSuccessListener(documentSnapshot -> {
            String currentUsername = removeAtGmail(documentSnapshot.getString("email"));
            if (comment.getUsername().equals(currentUsername)){
                deleteButton.setVisibility(View.VISIBLE);
            }
        });

        vh.commentsLayout.addView(ratingLayout,ratingLayoutParams);
        vh.commentsLayout.addView(commentLayout,layoutParams);
        View separator = new View(getContext());
        separator.setLayoutParams(new LinearLayout.LayoutParams(
                matchParent, 1 // Set the height you want for the separator
        ));
        separator.setBackgroundColor(Color.argb(128, 0, 0, 0));
        vh.commentsLayout.addView(separator);
    }

    private String removeAtGmail(String username){
        username = username.replace("@gmail.com", "");
        username = username.replace("@aucklanduni.ac.nz", "");
        return username;
    }


}